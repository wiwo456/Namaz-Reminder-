package com.hussain.namazreminder.prayer

data class PrayerTime(
    val name: String,
    val time24: String,
    val displayTime: String
)

data class PrayerSchedule(
    val readableDate: String,
    val hijriMonth: Int,
    val prayers: List<PrayerTime>
)
