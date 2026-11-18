package com.example.ui.viewer

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

class PdfVoiceReaderController(
  private val context: Context,
  private val onPageFinished: (Int) -> Unit = {},
) {
  private var tts: TextToSpeech? = null
  var isReady by mutableStateOf(false)
    private set
  var isSpeaking by mutableStateOf(false)
    private set
  var currentReadingPage by mutableIntStateOf(0)
    private set
  var speechRate by mutableFloatStateOf(0.95f)
  var voiceLanguage by mutableStateOf("en-IN")

  init {
    tts = TextToSpeech(context.applicationContext) { status ->
      if (status == TextToSpeech.SUCCESS) {
        applyIndianVoice()
        isReady = true
      }
    }
  }

  private fun applyIndianVoice() {
    val t = tts ?: return
    val targetLocale = if (voiceLanguage == "hi-IN") Locale("hi", "IN") else Locale("en", "IN")
    val res = t.setLanguage(targetLocale)
    if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
      // Fallback to UK English (commonwealth cadence) or US
      val fallback = t.setLanguage(Locale("en", "GB"))
      if (fallback == TextToSpeech.LANG_MISSING_DATA || fallback == TextToSpeech.LANG_NOT_SUPPORTED) {
        t.setLanguage(Locale.US)
      }
    }
    t.setPitch(1.02f)
    t.setSpeechRate(speechRate)

    t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
      override fun onStart(utteranceId: String?) {
        isSpeaking = true
      }
      override fun onDone(utteranceId: String?) {
        isSpeaking = false
        val page = utteranceId?.removePrefix("PDF_PAGE_")?.toIntOrNull()
        if (page != null) {
          onPageFinished(page)
        }
      }
      @Deprecated("Deprecated in Java")
      override fun onError(utteranceId: String?) {
        isSpeaking = false
      }
      override fun onError(utteranceId: String?, errorCode: Int) {
        isSpeaking = false
      }
    })
  }

  fun readPage(pageIndex: Int, text: String) {
    if (!isReady || text.isBlank()) return
    currentReadingPage = pageIndex
    tts?.setSpeechRate(speechRate)
    applyIndianVoice()
    val clean = text.replace(Regex("""\s+"""), " ").trim()
    tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "PDF_PAGE_$pageIndex")
    isSpeaking = true
  }

  fun togglePlayPause(pageIndex: Int, text: String) {
    if (isSpeaking) {
      stop()
    } else {
      readPage(pageIndex, text)
    }
  }

  fun stop() {
    tts?.stop()
    isSpeaking = false
  }

  fun setLanguage(lang: String) {
    voiceLanguage = lang
    applyIndianVoice()
  }

  fun setSpeed(speed: Float) {
    speechRate = speed
    tts?.setSpeechRate(speed)
  }

  fun shutdown() {
    tts?.stop()
    tts?.shutdown()
    tts = null
    isSpeaking = false
    isReady = false
  }
}
