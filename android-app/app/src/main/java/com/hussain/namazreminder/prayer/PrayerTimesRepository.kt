package com.hussain.namazreminder.prayer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class PrayerTimesRepository {
    suspend fun fetchPrayerSchedule(
        latitude: Double,
        longitude: Double,
        method: String
    ): PrayerSchedule =
        withContext(Dispatchers.IO) {
            val endpoint = buildString {
                append("https://api.aladhan.com/v1/timings")
                append("?latitude=")
                append(latitude)
                append("&longitude=")
                append(longitude)
                append("&method=")
                append(method)
            }

            val connection = URL(endpoint).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            try {
                val statusCode = connection.responseCode
                val stream = if (statusCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val body = stream.bufferedReader().use { it.readText() }

                if (statusCode !in 200..299) {
                    throw IllegalStateException("Prayer time request failed with HTTP $statusCode")
                }

                PrayerScheduleParser.parse(body)
            } finally {
                connection.disconnect()
            }
        }
}
