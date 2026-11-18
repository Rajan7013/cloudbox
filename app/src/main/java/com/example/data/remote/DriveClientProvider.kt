package com.example.data.remote

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import coil.ImageLoader
import com.example.auth.DriveAuthManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.BufferedSink
import okio.source
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class DriveApiException(
  val code: Int,
  override val message: String,
  val isAuthExpired: Boolean = code == 401,
  val isPermissionDenied: Boolean = code == 403,
) : IOException(message)

class DriveClientProvider(
  private val context: Context,
  private val authManager: DriveAuthManager,
) {
  val moshi: Moshi =
    Moshi.Builder()
      .addLast(KotlinJsonAdapterFactory())
      .build()

  private val authInterceptor = Interceptor { chain ->
    val token = authManager.currentTokenOrNull()
    val original = chain.request()
    val builder = original.newBuilder()
    if (!token.isNullOrBlank()) {
      builder.header("Authorization", "Bearer $token")
    }
    chain.proceed(builder.build())
  }

  val okHttpClient: OkHttpClient =
    OkHttpClient.Builder()
      .addInterceptor(authInterceptor)
      .connectTimeout(30, TimeUnit.SECONDS)
      .readTimeout(60, TimeUnit.SECONDS)
      .writeTimeout(120, TimeUnit.SECONDS)
      .build()

  val retrofit: Retrofit =
    Retrofit.Builder()
      .baseUrl("https://www.googleapis.com/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()

  val apiService: DriveApiService = retrofit.create(DriveApiService::class.java)

  val imageLoader: ImageLoader by lazy {
    ImageLoader.Builder(context.applicationContext)
      .okHttpClient(okHttpClient)
      .crossfade(200)
      .build()
  }

  /**
   * Executes a real Google Drive Resumable Upload session from a local ContentResolver Uri
   * with byte-level progress callbacks and cancellation support.
   */
  suspend fun uploadUriResumable(
    uri: Uri,
    fileName: String,
    mimeType: String,
    parentFolderId: String?,
    onProgress: (uploadedBytes: Long, totalBytes: Long) -> Unit,
    isCancelled: () -> Boolean,
  ): DriveFileDto =
    withContext(Dispatchers.IO) {
      val contentResolver = context.contentResolver
      val totalBytes = resolveContentLength(uri)

      val metadataJson =
        JSONObject().apply {
          put("name", fileName)
          if (!parentFolderId.isNullOrBlank() && parentFolderId != "root") {
            put("parents", JSONArray(listOf(parentFolderId)))
          }
        }

      val initUrl =
        "https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable&fields=${
          java.net.URLEncoder.encode(DriveApiService.FILE_FIELDS, "UTF-8")
        }"

      val initRequest =
        Request.Builder()
          .url(initUrl)
          .post(metadataJson.toString().toRequestBody("application/json; charset=UTF-8".toMediaTypeOrNull()))
          .header("X-Upload-Content-Type", mimeType)
          .apply {
            if (totalBytes > 0L) {
              header("X-Upload-Content-Length", totalBytes.toString())
            }
          }
          .build()

      val sessionUri: String =
        okHttpClient.newCall(initRequest).execute().use { resp ->
          if (!resp.isSuccessful) {
            throw parseDriveError(resp)
          }
          resp.header("Location")
            ?: throw DriveApiException(resp.code, "Upload session URL missing from Drive response.")
        }

      val streamingBody =
        object : RequestBody() {
          override fun contentType() = mimeType.toMediaTypeOrNull()

          override fun contentLength(): Long = if (totalBytes > 0L) totalBytes else -1L

          override fun writeTo(sink: BufferedSink) {
            val stream =
              contentResolver.openInputStream(uri)
                ?: throw IOException("Unable to read selected file.")
            stream.use { input ->
              val buffer = ByteArray(64 * 1024)
              var uploaded = 0L
              while (true) {
                if (isCancelled()) {
                  throw IOException("Upload cancelled")
                }
                val read = input.read(buffer)
                if (read == -1) break
                sink.write(buffer, 0, read)
                uploaded += read
                onProgress(uploaded, if (totalBytes > 0L) totalBytes else uploaded)
              }
              sink.flush()
            }
          }
        }

      val uploadRequest =
        Request.Builder()
          .url(sessionUri)
          .put(streamingBody)
          .build()

      okHttpClient.newCall(uploadRequest).execute().use { resp ->
        if (!resp.isSuccessful) {
          throw parseDriveError(resp)
        }
        val bodyStr = resp.body?.string().orEmpty()
        val adapter = moshi.adapter(DriveFileDto::class.java)
        adapter.fromJson(bodyStr)
          ?: throw DriveApiException(500, "Drive returned empty metadata after upload.")
      }
    }

  private fun resolveContentLength(uri: Uri): Long {
    if (uri.scheme == "file") {
      val f = uri.path?.let { java.io.File(it) }
      if (f != null && f.exists()) return f.length()
    }
    return try {
      context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
          val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
          if (idx >= 0 && !cursor.isNull(idx)) cursor.getLong(idx) else -1L
        } else {
          -1L
        }
      } ?: -1L
    } catch (_: Exception) {
      -1L
    }
  }

  fun parseErrorMessage(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return try {
      moshi.adapter(DriveErrorEnvelope::class.java).fromJson(raw)?.error?.message
    } catch (_: Exception) {
      null
    }
  }

  private fun parseDriveError(resp: Response): DriveApiException {
    val raw = resp.body?.string().orEmpty()
    val parsedMsg = parseErrorMessage(raw)
    return DriveApiException(
      code = resp.code,
      message = parsedMsg ?: friendlyMessageForCode(resp.code),
    )
  }

  companion object {
    fun friendlyMessageForCode(code: Int): String =
      when (code) {
        401 -> "Your session expired. Please reconnect Google Drive."
        403 -> "Access denied or Google Drive permission needs to be reconnected."
        404 -> "This item could not be found. It may have been moved or deleted."
        429 -> "Too many requests to Google Drive. Please wait a moment and try again."
        in 500..599 -> "Something went wrong on Google Drive’s side."
        else -> "Google Drive request failed (HTTP $code)."
      }
  }
}
