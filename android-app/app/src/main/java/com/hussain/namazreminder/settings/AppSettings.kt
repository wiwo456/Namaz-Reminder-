package com.hussain.namazreminder.settings

data class AppSettings(
    val homeLatitude: Double = 40.9364,
    val homeLongitude: Double = -74.1767,
    val currentLatitude: Double = 40.9364,
    val currentLongitude: Double = -74.1767,
    val locationSource: String = "Home coordinates",
    val method: String = "2",
    val notificationsEnabled: Boolean = false
)
