package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ln
import kotlin.math.pow

object FormatUtils {
  fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.lastIndex)
    val value = bytes / 1024.0.pow(digitGroups.toDouble())
    return if (digitGroups == 0) {
      "$bytes B"
    } else if (value >= 100.0) {
      String.format(Locale.US, "%.0f %s", value, units[digitGroups])
    } else {
      String.format(Locale.US, "%.1f %s", value, units[digitGroups])
    }
  }

  fun middleEllipsis(name: String, maxChars: Int = 45): String {
    if (name.length <= maxChars || maxChars < 8) return name
    val dotIndex = name.lastIndexOf('.')
    val ext = if (dotIndex in 1 until name.length - 1 && name.length - dotIndex <= 8) {
      name.substring(dotIndex)
    } else {
      ""
    }
    val tailKeep = (ext.length + 4).coerceAtMost(maxChars / 2)
    val headKeep = (maxChars - tailKeep - 1).coerceAtLeast(4)
    return "${name.take(headKeep)}…${name.takeLast(tailKeep)}"
  }

  fun parseIso8601ToEpochMs(iso: String?): Long {
    if (iso.isNullOrBlank()) return 0L
    val patterns =
      listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        "yyyy-MM-dd'T'HH:mm:ssX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
      )
    for (pattern in patterns) {
      try {
        val sdf = SimpleDateFormat(pattern, Locale.US).apply {
          timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = sdf.parse(iso)
        if (date != null) return date.time
      } catch (_: Exception) {
      }
    }
    return 0L
  }

  fun formatRelativeDate(epochMs: Long): String {
    if (epochMs <= 0L) return ""
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = epochMs }

    val sameYear = now.get(Calendar.YEAR) == target.get(Calendar.YEAR)
    val todayDay = now.get(Calendar.DAY_OF_YEAR)
    val targetDay = target.get(Calendar.DAY_OF_YEAR)

    return when {
      sameYear && todayDay == targetDay -> "Today"
      sameYear && todayDay - targetDay == 1 -> "Yesterday"
      sameYear -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(epochMs))
      else -> SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(epochMs))
    }
  }

  fun formatFullDateTime(epochMs: Long): String {
    if (epochMs <= 0L) return "Unknown"
    return SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(epochMs))
  }

  fun trashDaysRemaining(trashedTimeEpochMs: Long?, fallbackModifiedMs: Long): Int {
    val startMs = trashedTimeEpochMs?.takeIf { it > 0L } ?: fallbackModifiedMs
    if (startMs <= 0L) return 30
    val elapsedDays = ((System.currentTimeMillis() - startMs) / (1000L * 60L * 60L * 24L)).toInt()
    return (30 - elapsedDays).coerceIn(0, 30)
  }

  fun daysAgoGroup(epochMs: Long): String {
    if (epochMs <= 0L) return "EARLIER"
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = epochMs }
    val diffMs = (now.timeInMillis - target.timeInMillis).coerceAtLeast(0L)
    val diffDays = diffMs / (1000L * 60L * 60L * 24L)
    return when {
      now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR) -> "TODAY"
      diffDays <= 7L -> "THIS WEEK"
      else -> "EARLIER"
    }
  }
}
