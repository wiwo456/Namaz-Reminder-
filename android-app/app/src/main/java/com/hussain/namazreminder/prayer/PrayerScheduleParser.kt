package com.hussain.namazreminder.prayer

import org.json.JSONObject

object PrayerScheduleParser {
    private val prayerOrder = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")

    fun parse(json: String): PrayerSchedule {
        val root = JSONObject(json)
        val data = root.getJSONObject("data")
        val timings = data.getJSONObject("timings")
        val date = data.getJSONObject("date")
        val hijri = date.getJSONObject("hijri")
        val hijriMonth = hijri.getJSONObject("month").getInt("number")

        val prayers = prayerOrder.map { name ->
            val time24 = timings.getString(name).substringBefore(" ")
            PrayerTime(
                name = name,
                time24 = time24,
                displayTime = PrayerTimeFormatter.toDisplayTime(time24)
            )
        }

        return PrayerSchedule(
            readableDate = date.getString("readable"),
            hijriMonth = hijriMonth,
            prayers = prayers
        )
    }
}
