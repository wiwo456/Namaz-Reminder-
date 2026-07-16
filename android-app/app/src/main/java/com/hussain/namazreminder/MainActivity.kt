package com.hussain.namazreminder

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.hussain.namazreminder.location.DeviceLocationProvider
import com.hussain.namazreminder.notification.PrayerAlarmScheduler
import com.hussain.namazreminder.notification.PrayerNotificationHelper
import com.hussain.namazreminder.prayer.CountdownFormatter
import com.hussain.namazreminder.prayer.PrayerSchedule
import com.hussain.namazreminder.prayer.PrayerScheduleCalculator
import com.hussain.namazreminder.prayer.PrayerTime
import com.hussain.namazreminder.prayer.PrayerTimesRepository
import com.hussain.namazreminder.settings.AppSettings
import com.hussain.namazreminder.settings.SettingsRepository
import com.hussain.namazreminder.widget.PrayerWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val countdownHandler = Handler(Looper.getMainLooper())

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var prayerTimesRepository: PrayerTimesRepository
    private lateinit var deviceLocationProvider: DeviceLocationProvider
    private lateinit var prayerAlarmScheduler: PrayerAlarmScheduler
    private lateinit var notificationHelper: PrayerNotificationHelper

    private lateinit var dateText: TextView
    private lateinit var nextPrayerNameText: TextView
    private lateinit var nextPrayerTimeText: TextView
    private lateinit var countdownText: TextView
    private lateinit var widgetPreviewNameText: TextView
    private lateinit var widgetPreviewTimeText: TextView
    private lateinit var widgetPreviewCountdownText: TextView
    private lateinit var methodInput: AutoCompleteTextView
    private lateinit var notificationsSwitch: MaterialSwitch
    private lateinit var useDeviceLocationButton: MaterialButton
    private lateinit var useHomeButton: MaterialButton
    private lateinit var saveHomeButton: MaterialButton
    private lateinit var loadPrayerTimesButton: MaterialButton
    private lateinit var settingsLocationSummaryText: TextView
    private lateinit var statusText: TextView
    private lateinit var locationSummaryText: TextView
    private lateinit var prayerListContainer: LinearLayout

    private var appSettings = AppSettings()
    private var currentSchedule: PrayerSchedule? = null
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            loadDeviceLocation()
        } else {
            setStatus(getString(R.string.location_permission_denied))
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scheduleNotificationsIfPossible()
        } else {
            notificationsSwitch.isChecked = false
            appSettings = appSettings.copy(notificationsEnabled = false)
            settingsRepository.save(appSettings)
            setStatus(getString(R.string.notification_permission_denied))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        settingsRepository = SettingsRepository(this)
        prayerTimesRepository = PrayerTimesRepository()
        deviceLocationProvider = DeviceLocationProvider(this)
        prayerAlarmScheduler = PrayerAlarmScheduler(this)
        notificationHelper = PrayerNotificationHelper(this)

        notificationHelper.createChannel()
        bindViews()
        configureMethodDropdown()
        bindEvents()

        appSettings = settingsRepository.load()
        applySettingsToForm()
        setStatus(getString(R.string.status_ready))
        loadPrayerTimes()
    }

    override fun onDestroy() {
        countdownHandler.removeCallbacksAndMessages(null)
        scope.cancel()
        super.onDestroy()
    }

    private fun bindViews() {
        dateText = findViewById(R.id.dateText)
        nextPrayerNameText = findViewById(R.id.nextPrayerNameText)
        nextPrayerTimeText = findViewById(R.id.nextPrayerTimeText)
        countdownText = findViewById(R.id.countdownText)
        widgetPreviewNameText = findViewById(R.id.widgetPreviewNameText)
        widgetPreviewTimeText = findViewById(R.id.widgetPreviewTimeText)
        widgetPreviewCountdownText = findViewById(R.id.widgetPreviewCountdownText)
        methodInput = findViewById(R.id.methodInput)
        notificationsSwitch = findViewById(R.id.notificationsSwitch)
        useDeviceLocationButton = findViewById(R.id.useDeviceLocationButton)
        useHomeButton = findViewById(R.id.useHomeButton)
        saveHomeButton = findViewById(R.id.saveHomeButton)
        loadPrayerTimesButton = findViewById(R.id.loadPrayerTimesButton)
        settingsLocationSummaryText = findViewById(R.id.settingsLocationSummaryText)
        statusText = findViewById(R.id.statusText)
        locationSummaryText = findViewById(R.id.locationSummaryText)
        prayerListContainer = findViewById(R.id.prayerListContainer)
    }

    private fun configureMethodDropdown() {
        val labels = METHOD_OPTIONS.map { it.label }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, labels)
        methodInput.setAdapter(adapter)
    }

    private fun bindEvents() {
        useDeviceLocationButton.setOnClickListener {
            if (deviceLocationProvider.hasPermission()) {
                loadDeviceLocation()
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }

        useHomeButton.setOnClickListener {
            appSettings = appSettings.copy(
                currentLatitude = appSettings.homeLatitude,
                currentLongitude = appSettings.homeLongitude,
                locationSource = getString(R.string.home_coordinates_source)
            )
            settingsRepository.save(appSettings)
            applySettingsToForm()
            setStatus(getString(R.string.home_coordinates_loaded))
        }

        saveHomeButton.setOnClickListener {
            appSettings = appSettings.copy(
                homeLatitude = appSettings.currentLatitude,
                homeLongitude = appSettings.currentLongitude,
                locationSource = getString(R.string.home_coordinates_source)
            )
            settingsRepository.save(appSettings)
            applySettingsToForm()
            setStatus(getString(R.string.home_coordinates_saved))
        }

        loadPrayerTimesButton.setOnClickListener {
            loadPrayerTimes()
        }

        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            appSettings = appSettings.copy(notificationsEnabled = isChecked)
            settingsRepository.save(appSettings)
            if (isChecked) {
                requestNotificationPermissionIfNeeded()
                scheduleNotificationsIfPossible()
            } else {
                prayerAlarmScheduler.cancelAll()
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun applySettingsToForm() {
        methodInput.setText(methodLabelFor(appSettings.method), false)
        notificationsSwitch.isChecked = appSettings.notificationsEnabled
        val locationSummary = getString(
            R.string.location_summary_value,
            appSettings.locationSource,
            formatCoordinate(appSettings.currentLatitude),
            formatCoordinate(appSettings.currentLongitude)
        )
        locationSummaryText.text = locationSummary
        settingsLocationSummaryText.text = locationSummary
        dateText.text = SimpleDateFormat("MMM d", Locale.US).format(Date())
    }

    private fun loadDeviceLocation() {
        scope.launch {
            setStatus(getString(R.string.loading_location))
            val coordinates = withContext(Dispatchers.IO) {
                deviceLocationProvider.getCurrentCoordinates()
            }

            if (coordinates == null) {
                setStatus(getString(R.string.location_unavailable))
                return@launch
            }

            appSettings = appSettings.copy(
                currentLatitude = coordinates.latitude,
                currentLongitude = coordinates.longitude,
                locationSource = getString(R.string.device_location_source)
            )
            settingsRepository.save(appSettings)
            applySettingsToForm()
            setStatus(getString(R.string.device_location_loaded))
        }
    }

    private fun loadPrayerTimes() {
        val method = methodValueFor(methodInput.text?.toString().orEmpty())
        appSettings = appSettings.copy(
            method = method
        )
        settingsRepository.save(appSettings)
        applySettingsToForm()

        scope.launch {
            setStatus(getString(R.string.loading_prayer_times))
            try {
                val schedule = prayerTimesRepository.fetchPrayerSchedule(
                    appSettings.currentLatitude,
                    appSettings.currentLongitude,
                    method
                )
                currentSchedule = schedule
                renderSchedule(schedule)

                if (appSettings.notificationsEnabled) {
                    requestNotificationPermissionIfNeeded()
                    scheduleNotificationsIfPossible(schedule)
                }

                setStatus(getString(R.string.loaded_schedule_status, schedule.readableDate))
            } catch (error: Exception) {
                setStatus(getString(R.string.loading_error, error.message ?: "Unknown error"))
            }
        }
    }

    private fun renderSchedule(schedule: PrayerSchedule) {
        dateText.text = schedule.readableDate
        prayerListContainer.removeAllViews()

        val nextPrayer = PrayerScheduleCalculator.findNextPrayer(schedule)
        val nextPrayerDateTime = PrayerScheduleCalculator.nextPrayerDateTime(nextPrayer, LocalDateTime.now())
        val countdown = CountdownFormatter.toCountdown(nextPrayerDateTime, LocalDateTime.now())

        nextPrayerNameText.text = nextPrayer.name
        nextPrayerTimeText.text = nextPrayer.displayTime
        countdownText.text = countdownTextFor(nextPrayer.name, countdown)

        widgetPreviewNameText.text = nextPrayer.name
        widgetPreviewTimeText.text = nextPrayer.displayTime
        widgetPreviewCountdownText.text = countdown

        PrayerWidgetUpdater.save(this, nextPrayer.name, nextPrayer.displayTime, countdown)

        schedule.prayers.forEach { prayer ->
            prayerListContainer.addView(createPrayerRow(prayer, prayer.name == nextPrayer.name))
        }

        startCountdownRefresh()
    }

    private fun startCountdownRefresh() {
        countdownHandler.removeCallbacksAndMessages(null)

        val updateRunnable = object : Runnable {
            override fun run() {
                currentSchedule?.let { schedule ->
                    val nextPrayer = PrayerScheduleCalculator.findNextPrayer(schedule)
                    val nextPrayerDateTime = PrayerScheduleCalculator.nextPrayerDateTime(
                        nextPrayer,
                        LocalDateTime.now()
                    )
                    val countdown = CountdownFormatter.toCountdown(
                        nextPrayerDateTime,
                        LocalDateTime.now()
                    )
                    countdownText.text = countdownTextFor(nextPrayer.name, countdown)
                    widgetPreviewNameText.text = nextPrayer.name
                    widgetPreviewTimeText.text = nextPrayer.displayTime
                    widgetPreviewCountdownText.text = countdown
                    PrayerWidgetUpdater.save(
                        this@MainActivity,
                        nextPrayer.name,
                        nextPrayer.displayTime,
                        countdown
                    )
                }

                countdownHandler.postDelayed(this, 30_000L)
            }
        }

        countdownHandler.post(updateRunnable)
    }

    private fun createPrayerRow(prayer: PrayerTime, isActive: Boolean): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 20, 24, 20)
            background = ContextCompat.getDrawable(
                this@MainActivity,
                if (isActive) R.drawable.prayer_row_active else R.drawable.prayer_row_background
            )
        }

        val nameText = TextView(this).apply {
            text = prayer.name
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.forest))
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val timeText = TextView(this).apply {
            text = prayer.displayTime
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.slate))
            textSize = 15f
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = 12
        }

        row.layoutParams = params
        row.addView(nameText)
        row.addView(timeText)
        return row
    }

    private fun methodLabelFor(method: String): String {
        return METHOD_OPTIONS.firstOrNull { it.value == method }?.label ?: METHOD_OPTIONS.first().label
    }

    private fun methodValueFor(label: String): String {
        return METHOD_OPTIONS.firstOrNull { it.label == label }?.value ?: "2"
    }

    private fun countdownTextFor(prayerName: String, countdown: String): String {
        return if (countdown == "Now") {
            getString(R.string.prayer_time_now, prayerName)
        } else {
            getString(R.string.prayer_countdown_format, prayerName, countdown)
        }
    }

    private fun formatCoordinate(value: Double): String {
        return String.format(Locale.US, "%.4f", value)
    }

    private fun setStatus(message: String) {
        statusText.text = message
    }

    private fun scheduleNotificationsIfPossible(schedule: PrayerSchedule? = currentSchedule) {
        if (!appSettings.notificationsEnabled) {
            prayerAlarmScheduler.cancelAll()
            return
        }

        if (!hasNotificationPermission()) {
            return
        }

        if (!prayerAlarmScheduler.canScheduleExactAlarms()) {
            setStatus(getString(R.string.exact_alarm_permission_required))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
            return
        }

        if (schedule == null) {
            return
        }

        prayerAlarmScheduler.scheduleDailyReminders(appSettings, schedule)
        setStatus(getString(R.string.notifications_scheduled))
    }

    private fun hasNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private data class MethodOption(val value: String, val label: String)

    companion object {
        private val METHOD_OPTIONS = listOf(
            MethodOption("2", "ISNA"),
            MethodOption("1", "University of Islamic Sciences, Karachi"),
            MethodOption("3", "Muslim World League"),
            MethodOption("4", "Umm Al-Qura"),
            MethodOption("5", "Egyptian General Authority")
        )
    }
}
