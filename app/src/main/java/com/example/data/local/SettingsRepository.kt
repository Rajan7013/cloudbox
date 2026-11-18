package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.CloudBoxSettings
import com.example.model.SortField
import com.example.model.ThemePreference
import com.example.model.ViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.cloudBoxDataStore: DataStore<Preferences> by preferencesDataStore(name = "cloudbox_settings")

class SettingsRepository(private val context: Context) {
  private object Keys {
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    val THEME = stringPreferencesKey("theme_preference")
    val DEFAULT_VIEW = stringPreferencesKey("default_view")
    val DEFAULT_SORT_FIELD = stringPreferencesKey("default_sort_field")
    val DEFAULT_SORT_ASC = booleanPreferencesKey("default_sort_asc")
    val WIFI_ONLY_UPLOADS = booleanPreferencesKey("wifi_only_uploads")
    val AUTOPLAY_MEDIA = booleanPreferencesKey("autoplay_media")
    val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    val NOTIFY_NEW_SHARE = booleanPreferencesKey("notify_new_share")
    val NOTIFY_UPLOAD_FINISHED = booleanPreferencesKey("notify_upload_finished")
    val NOTIFY_STORAGE_FULL = booleanPreferencesKey("notify_storage_full")
  }

  val settingsFlow: Flow<CloudBoxSettings> =
    context.cloudBoxDataStore.data.map { prefs ->
      CloudBoxSettings(
        onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
        themePreference =
          prefs[Keys.THEME]?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() }
            ?: ThemePreference.SYSTEM,
        defaultViewMode =
          prefs[Keys.DEFAULT_VIEW]?.let { runCatching { ViewMode.valueOf(it) }.getOrNull() }
            ?: ViewMode.GRID,
        defaultSortField =
          prefs[Keys.DEFAULT_SORT_FIELD]?.let { runCatching { SortField.valueOf(it) }.getOrNull() }
            ?: SortField.MODIFIED,
        defaultSortAscending = prefs[Keys.DEFAULT_SORT_ASC] ?: false,
        wifiOnlyUploads = prefs[Keys.WIFI_ONLY_UPLOADS] ?: false,
        autoplayMedia = prefs[Keys.AUTOPLAY_MEDIA] ?: true,
        keepScreenOnViewer = prefs[Keys.KEEP_SCREEN_ON] ?: false,
        notifyNewShare = prefs[Keys.NOTIFY_NEW_SHARE] ?: true,
        notifyUploadFinished = prefs[Keys.NOTIFY_UPLOAD_FINISHED] ?: true,
        notifyStorageAlmostFull = prefs[Keys.NOTIFY_STORAGE_FULL] ?: true,
      )
    }

  suspend fun setOnboardingCompleted(completed: Boolean) {
    context.cloudBoxDataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
  }

  suspend fun setThemePreference(theme: ThemePreference) {
    context.cloudBoxDataStore.edit { it[Keys.THEME] = theme.name }
  }

  suspend fun setDefaultViewMode(mode: ViewMode) {
    context.cloudBoxDataStore.edit { it[Keys.DEFAULT_VIEW] = mode.name }
  }

  suspend fun setDefaultSort(field: SortField, ascending: Boolean) {
    context.cloudBoxDataStore.edit {
      it[Keys.DEFAULT_SORT_FIELD] = field.name
      it[Keys.DEFAULT_SORT_ASC] = ascending
    }
  }

  suspend fun setWifiOnlyUploads(enabled: Boolean) {
    context.cloudBoxDataStore.edit { it[Keys.WIFI_ONLY_UPLOADS] = enabled }
  }

  suspend fun setAutoplayMedia(enabled: Boolean) {
    context.cloudBoxDataStore.edit { it[Keys.AUTOPLAY_MEDIA] = enabled }
  }

  suspend fun setKeepScreenOnViewer(enabled: Boolean) {
    context.cloudBoxDataStore.edit { it[Keys.KEEP_SCREEN_ON] = enabled }
  }

  suspend fun setNotifyNewShare(enabled: Boolean) {
    context.cloudBoxDataStore.edit { it[Keys.NOTIFY_NEW_SHARE] = enabled }
  }

  suspend fun setNotifyUploadFinished(enabled: Boolean) {
    context.cloudBoxDataStore.edit { it[Keys.NOTIFY_UPLOAD_FINISHED] = enabled }
  }

  suspend fun setNotifyStorageAlmostFull(enabled: Boolean) {
    context.cloudBoxDataStore.edit { it[Keys.NOTIFY_STORAGE_FULL] = enabled }
  }
}
