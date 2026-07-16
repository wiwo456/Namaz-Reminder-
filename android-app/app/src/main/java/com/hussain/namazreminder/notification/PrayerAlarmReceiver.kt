package com.hussain.namazreminder.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PRAYER_ALARM) {
            return
        }

        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: return
        PrayerNotificationHelper(context).createChannel()
        PrayerNotificationHelper(context).showPrayerNotification(prayerName)
    }

    companion object {
        const val ACTION_PRAYER_ALARM = "com.hussain.namazreminder.action.PRAYER_ALARM"
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
    }
}
