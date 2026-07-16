package com.hussain.namazreminder.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hussain.namazreminder.prayer.PrayerTimesRepository
import com.hussain.namazreminder.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PrayerScheduleRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settingsRepository = SettingsRepository(appContext)
                val settings = settingsRepository.load()
                val scheduler = PrayerAlarmScheduler(appContext)

                if (!settings.notificationsEnabled) {
                    scheduler.cancelAll()
                    return@launch
                }

                val schedule = PrayerTimesRepository().fetchPrayerSchedule(
                    settings.currentLatitude,
                    settings.currentLongitude,
                    settings.method
                )
                scheduler.scheduleDailyReminders(settings, schedule)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_SCHEDULE = "com.hussain.namazreminder.action.REFRESH_SCHEDULE"
        const val REFRESH_REQUEST_CODE = 9000
        val PRAYER_REQUEST_CODES = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
    }
}
