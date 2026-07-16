package com.hussain.namazreminder.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.hussain.namazreminder.prayer.PrayerSchedule
import com.hussain.namazreminder.settings.AppSettings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class PrayerAlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val timeFormatter = DateTimeFormatter.ofPattern("H:mm", Locale.US)

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun cancelAll() {
        PrayerScheduleRefreshReceiver.PRAYER_REQUEST_CODES.forEachIndexed { index, _ ->
            alarmManager.cancel(prayerPendingIntent(index, null))
        }
        alarmManager.cancel(refreshPendingIntent())
    }

    fun scheduleDailyReminders(settings: AppSettings, schedule: PrayerSchedule) {
        cancelAll()

        if (!settings.notificationsEnabled || !canScheduleExactAlarms()) {
            return
        }

        schedule.prayers.forEachIndexed { index, prayer ->
            val triggerAtMillis = nextPrayerTimeInMillis(prayer.time24)
            if (triggerAtMillis <= System.currentTimeMillis()) {
                return@forEachIndexed
            }

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                prayerPendingIntent(index, prayer.name)
            )
        }

        scheduleRefresh()
    }

    private fun scheduleRefresh() {
        val refreshTime = LocalDate.now()
            .plusDays(1)
            .atStartOfDay()
            .plusMinutes(1)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            refreshTime,
            refreshPendingIntent()
        )
    }

    private fun prayerPendingIntent(requestCode: Int, prayerName: String?): PendingIntent {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
            if (prayerName != null) {
                putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayerName)
            }
        }

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun refreshPendingIntent(): PendingIntent {
        val intent = Intent(context, PrayerScheduleRefreshReceiver::class.java).apply {
            action = PrayerScheduleRefreshReceiver.ACTION_REFRESH_SCHEDULE
        }

        return PendingIntent.getBroadcast(
            context,
            PrayerScheduleRefreshReceiver.REFRESH_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextPrayerTimeInMillis(time24: String): Long {
        val prayerTime = LocalTime.parse(time24, timeFormatter)
        val dateTime = LocalDateTime.now()
            .withHour(prayerTime.hour)
            .withMinute(prayerTime.minute)
            .withSecond(0)
            .withNano(0)

        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
