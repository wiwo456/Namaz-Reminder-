package com.hussain.namazreminder.prayer

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object PrayerTimeFormatter {
    private val inputFormatter = DateTimeFormatter.ofPattern("H:mm", Locale.US)
    private val outputFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

    fun toDisplayTime(time24: String): String {
        val localTime = LocalTime.parse(time24, inputFormatter)
        return localTime.format(outputFormatter)
    }
}
