package com.hussain.namazreminder.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.hussain.namazreminder.prayer.PrayerSchedule
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class PrayerAlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val timeFormatter = DateTimeFormatter.ofPattern("H:mm", Locale.US)

    fun scheduleToday(schedule: PrayerSchedule) {
        schedule.prayers.forEachIndexed { index, prayer ->
            val triggerAtMillis = nextPrayerTimeInMillis(prayer.time24)
            if (triggerAtMillis <= System.currentTimeMillis()) {
                return@forEachIndexed
            }

            val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayer.name)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                index,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
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
