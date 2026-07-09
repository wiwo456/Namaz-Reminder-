package com.hussain.namazreminder.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.hussain.namazreminder.R

object PrayerWidgetUpdater {
    private const val PREFS_NAME = "namaz_widget_data"
    private const val KEY_NAME = "next_prayer_name"
    private const val KEY_TIME = "next_prayer_time"
    private const val KEY_COUNTDOWN = "next_prayer_countdown"

    fun save(context: Context, nextPrayerName: String, nextPrayerTime: String, countdown: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NAME, nextPrayerName)
            .putString(KEY_TIME, nextPrayerTime)
            .putString(KEY_COUNTDOWN, countdown)
            .apply()

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, PrayerWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        updateAll(context, appWidgetManager, appWidgetIds)
    }

    fun updateAll(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = preferences.getString(KEY_NAME, context.getString(R.string.waiting_label))
            ?: context.getString(R.string.waiting_label)
        val time = preferences.getString(KEY_TIME, context.getString(R.string.time_placeholder))
            ?: context.getString(R.string.time_placeholder)
        val countdown = preferences.getString(
            KEY_COUNTDOWN,
            context.getString(R.string.countdown_placeholder)
        ) ?: context.getString(R.string.countdown_placeholder)

        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_prayer).apply {
                setTextViewText(R.id.widgetNextPrayerNameText, name)
                setTextViewText(R.id.widgetNextPrayerTimeText, time)
                setTextViewText(R.id.widgetNextPrayerCountdownText, countdown)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
