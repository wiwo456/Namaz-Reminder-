package com.hussain.namazreminder.prayer

import java.time.Duration
import java.time.LocalDateTime

object CountdownFormatter {
    fun toCountdown(target: LocalDateTime, now: LocalDateTime = LocalDateTime.now()): String {
        val minutes = Duration.between(now, target).toMinutes()
        if (minutes <= 0) {
            return "Now"
        }

        val hours = minutes / 60
        val remainder = minutes % 60

        return if (hours == 0L) {
            "in ${remainder}m"
        } else {
            "in ${hours}h ${remainder}m"
        }
    }
}
