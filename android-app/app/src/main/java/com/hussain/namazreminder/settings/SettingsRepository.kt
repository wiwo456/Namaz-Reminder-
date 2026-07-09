package com.hussain.namazreminder.settings

import android.content.Context

class SettingsRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): AppSettings {
        return AppSettings(
            homeLatitude = preferences.getString(KEY_HOME_LAT, null)?.toDoubleOrNull() ?: 40.9364,
            homeLongitude = preferences.getString(KEY_HOME_LON, null)?.toDoubleOrNull() ?: -74.1767,
            currentLatitude = preferences.getString(KEY_CURRENT_LAT, null)?.toDoubleOrNull() ?: 40.9364,
            currentLongitude = preferences.getString(KEY_CURRENT_LON, null)?.toDoubleOrNull() ?: -74.1767,
            locationSource = preferences.getString(KEY_SOURCE, "Home coordinates") ?: "Home coordinates",
            method = preferences.getString(KEY_METHOD, "2") ?: "2",
            notificationsEnabled = preferences.getBoolean(KEY_NOTIFICATIONS, false)
        )
    }

    fun save(settings: AppSettings) {
        preferences.edit()
            .putString(KEY_HOME_LAT, settings.homeLatitude.toString())
            .putString(KEY_HOME_LON, settings.homeLongitude.toString())
            .putString(KEY_CURRENT_LAT, settings.currentLatitude.toString())
            .putString(KEY_CURRENT_LON, settings.currentLongitude.toString())
            .putString(KEY_SOURCE, settings.locationSource)
            .putString(KEY_METHOD, settings.method)
            .putBoolean(KEY_NOTIFICATIONS, settings.notificationsEnabled)
            .apply()
    }

    companion object {
        const val PREFS_NAME = "namaz_reminder_settings"
        private const val KEY_HOME_LAT = "home_latitude"
        private const val KEY_HOME_LON = "home_longitude"
        private const val KEY_CURRENT_LAT = "current_latitude"
        private const val KEY_CURRENT_LON = "current_longitude"
        private const val KEY_SOURCE = "location_source"
        private const val KEY_METHOD = "calculation_method"
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
    }
}
