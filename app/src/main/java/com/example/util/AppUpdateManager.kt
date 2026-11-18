package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class AppUpdateInfo(
  val versionCode: Int,
  val versionName: String,
  val apkUrl: String,
  val changeLog: String,
  val isMandatory: Boolean = false,
)

class AppUpdateManager(
  private val context: Context,
  private val okHttpClient: OkHttpClient = OkHttpClient.Builder().build(),
) {
  companion object {
    private const val TAG = "AppUpdateManager"
    // Pinned official URLs for update metadata
    private val UPDATE_ENDPOINTS = listOf(
      "https://raw.githubusercontent.com/Rajan7013/cloudbox/main/website/version.json",
      "https://cloudbox-woad.vercel.app/version.json"
    )
  }

  suspend fun checkForUpdate(): AppUpdateInfo? = withContext(Dispatchers.IO) {
    for (url in UPDATE_ENDPOINTS) {
      try {
        val request = Request.Builder()
          .url(url)
          .header("Cache-Control", "no-cache")
          .build()
        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful) {
          val bodyString = response.body?.string() ?: continue
          val json = JSONObject(bodyString)
          val remoteVersionCode = json.optInt("versionCode", 0)
          val remoteVersionName = json.optString("versionName", "")
          val apkUrl = json.optString("apkUrl", "")
          val changeLog = json.optString("changeLog", "Bug fixes and performance improvements.")
          val isMandatory = json.optBoolean("isMandatory", false)

          // Security check: Must be strict HTTPS URL
          if (!apkUrl.startsWith("https://")) {
            Log.w(TAG, "Rejected insecure update URL: $apkUrl")
            continue
          }

          if (remoteVersionCode > BuildConfig.VERSION_CODE) {
            return@withContext AppUpdateInfo(
              versionCode = remoteVersionCode,
              versionName = remoteVersionName,
              apkUrl = apkUrl,
              changeLog = changeLog,
              isMandatory = isMandatory,
            )
          }
        }
      } catch (e: Exception) {
        Log.d(TAG, "Check update failed for $url: ${e.message}")
      }
    }
    null
  }

  suspend fun downloadApk(
    apkUrl: String,
    onProgress: (Float) -> Unit,
  ): File = withContext(Dispatchers.IO) {
    val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
    val apkFile = File(updatesDir, "CloudBox-update.apk")
    if (apkFile.exists()) apkFile.delete()

    val request = Request.Builder().url(apkUrl).build()
    val response = okHttpClient.newCall(request).execute()
    if (!response.isSuccessful) throw IllegalStateException("Download failed with HTTP ${response.code}")

    val body = response.body ?: throw IllegalStateException("Empty response body")
    val contentLength = body.contentLength()

    body.byteStream().use { input ->
      FileOutputStream(apkFile).use { output ->
        val buffer = ByteArray(8192)
        var bytesRead: Int
        var totalBytesRead = 0L
        while (input.read(buffer).also { bytesRead = it } != -1) {
          output.write(buffer, 0, bytesRead)
          totalBytesRead += bytesRead
          if (contentLength > 0) {
            onProgress((totalBytesRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f))
          }
        }
        output.flush()
      }
    }

    if (apkFile.length() < 100_000L) {
      apkFile.delete()
      throw IllegalStateException("Downloaded file is too small to be a valid APK.")
    }

    apkFile
  }

  fun triggerInstall(apkFile: File) {
    val uri = FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      apkFile
    )
    val intent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(uri, "application/vnd.android.package-archive")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
  }
}
