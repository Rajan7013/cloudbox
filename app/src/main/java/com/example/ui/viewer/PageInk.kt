package com.example.ui.viewer

import android.content.SharedPreferences
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.json.JSONArray

/** One entry per annotated page. Coordinates and stroke width are fractions of page dimensions. */
internal class PageInk(private val preferences: SharedPreferences) {
  fun load(page: Int): List<DrawnStroke> {
    val raw = preferences.getString("ink:$page", null) ?: return emptyList()
    return try {
      val data = JSONArray(raw)
      List(data.length()) { i ->
        val stroke = data.getJSONArray(i)
        val coordinates = stroke.getJSONArray(4)
        val points = List(coordinates.length() / 2) { n ->
          Offset(coordinates.getDouble(n * 2).toFloat(), coordinates.getDouble(n * 2 + 1).toFloat())
        }
        DrawnStroke(points, Color(stroke.getInt(0)), stroke.getDouble(1).toFloat(), stroke.getBoolean(2), PenTip.valueOf(stroke.getString(3)))
      }
    } catch (_: org.json.JSONException) { emptyList() }
  }

  fun save(page: Int, strokes: List<DrawnStroke>) {
    val key = "ink:$page"
    if (strokes.isEmpty()) { preferences.edit().remove(key).apply(); return }
    val data = JSONArray()
    for (stroke in strokes) {
      val points = JSONArray()
      stroke.points.forEach { points.put(it.x.toDouble()).put(it.y.toDouble()) }
      data.put(JSONArray().put(stroke.color.toArgb()).put(stroke.strokeWidth.toDouble())
        .put(stroke.isHighlighter).put(stroke.tip.name).put(points))
    }
    preferences.edit().putString(key, data.toString()).apply()
  }
}
