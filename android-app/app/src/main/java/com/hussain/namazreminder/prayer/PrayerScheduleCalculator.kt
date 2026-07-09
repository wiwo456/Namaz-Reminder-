package com.hussain.namazreminder.prayer

import java.time.LocalTime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object PrayerScheduleCalculator {
    private val timeFormatter = DateTimeFormatter.ofPattern("H:mm", Locale.US)

    fun findNextPrayer(schedule: PrayerSchedule, now: LocalTime = LocalTime.now()): PrayerTime {
        return schedule.prayers.firstOrNull { prayer ->
            LocalTime.parse(prayer.time24, timeFormatter).isAfter(now)
        } ?: schedule.prayers.first()
    }

    fun nextPrayerDateTime(
        prayer: PrayerTime,
        now: LocalDateTime = LocalDateTime.now()
    ): LocalDateTime {
        var target = now.withHour(LocalTime.parse(prayer.time24, timeFormatter).hour)
            .withMinute(LocalTime.parse(prayer.time24, timeFormatter).minute)
            .withSecond(0)
            .withNano(0)

        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }

        return target
    }
}
