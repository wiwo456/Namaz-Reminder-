const STORAGE_KEY = "namaz-reminder-web-settings";
const ALADHAN_METHOD = "2";
const WEATHER_REFRESH_MS = 10 * 60 * 1000;
const DEFAULT_SETTINGS = {
    homeLatitude: 40.9364,
    homeLongitude: -74.1767,
    homeLocationName: null,
    homeLocationKey: null,
    currentLatitude: 40.9364,
    currentLongitude: -74.1767,
    currentLocationName: null,
    currentLocationKey: null,
    locationSource: "Home coordinates",
    notificationsEnabled: false,
};
const PRAYER_ORDER = ["Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"];

const elements = {
    todayLabel: document.getElementById("todayLabel"),
    menuButton: document.getElementById("menuButton"),
    menuDropdown: document.getElementById("menuDropdown"),
    settingsMenuItem: document.getElementById("settingsMenuItem"),
    backButton: document.getElementById("backButton"),
    installButton: document.getElementById("installButton"),
    installStatus: document.getElementById("installStatus"),
    heroCard: document.getElementById("heroCard"),
    nextPrayerName: document.getElementById("nextPrayerName"),
    nextPrayerTime: document.getElementById("nextPrayerTime"),
    countdownText: document.getElementById("countdownText"),
    weatherTemp: document.getElementById("weatherTemp"),
    weatherStatus: document.getElementById("weatherStatus"),
    weatherMeta: document.getElementById("weatherMeta"),
    notificationsToggle: document.getElementById("notificationsToggle"),
    useLocationButton: document.getElementById("useLocationButton"),
    useSavedButton: document.getElementById("useSavedButton"),
    saveHomeButton: document.getElementById("saveHomeButton"),
    loadTimesButton: document.getElementById("loadTimesButton"),
    statusText: document.getElementById("statusText"),
    settingsLocationSummary: document.getElementById("settingsLocationSummary"),
    locationSummary: document.getElementById("locationSummary"),
    prayerList: document.getElementById("prayerList"),
};

let appSettings = loadSettings();
let currentSchedule = null;
let nextPrayerTimer = null;
let reminderTimeoutId = null;
let lastReminderPrayer = null;
let deferredInstallPrompt = null;
let menuOpen = false;
let weatherRefreshTimer = null;

function isIosBrowser() {
    return /iPhone|iPad|iPod/i.test(window.navigator.userAgent);
}

function loadSettings() {
    try {
        const raw = localStorage.getItem(STORAGE_KEY);
        return raw ? { ...DEFAULT_SETTINGS, ...JSON.parse(raw) } : { ...DEFAULT_SETTINGS };
    } catch {
        return { ...DEFAULT_SETTINGS };
    }
}

function saveSettings() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(appSettings));
}

function setStatus(message) {
    if (elements.statusText) {
        elements.statusText.textContent = message;
    }
}

function setInstallStatus(message) {
    if (elements.installStatus) {
        elements.installStatus.textContent = message;
    }
}

function setWeatherDisplay(summary) {
    if (elements.heroCard && summary.theme) {
        elements.heroCard.dataset.weatherTheme = summary.theme;
    }
    if (elements.weatherTemp && summary.temperatureText) {
        elements.weatherTemp.textContent = summary.temperatureText;
    }
    if (elements.weatherStatus && summary.statusText) {
        elements.weatherStatus.textContent = summary.statusText;
    }
    if (elements.weatherMeta && summary.metaText) {
        elements.weatherMeta.textContent = summary.metaText;
    }
}

function setMenuOpen(isOpen) {
    if (!elements.menuDropdown || !elements.menuButton) {
        return;
    }

    menuOpen = isOpen;
    elements.menuDropdown.classList.toggle("hidden-panel", !isOpen);
    elements.menuDropdown.setAttribute("aria-hidden", String(!isOpen));
    elements.menuButton.setAttribute("aria-expanded", String(isOpen));
}

function setFormFromSettings() {
    const locationText = getCurrentLocationLabel();

    if (elements.notificationsToggle) {
        elements.notificationsToggle.checked = appSettings.notificationsEnabled;
    }
    if (elements.settingsLocationSummary) {
        elements.settingsLocationSummary.textContent = locationText;
    }
    if (elements.locationSummary) {
        elements.locationSummary.textContent = locationText;
    }
    if (elements.todayLabel) {
        elements.todayLabel.textContent = new Date().toLocaleDateString([], {
            month: "short",
            day: "numeric",
        });
    }
}

function formatCoordinates(latitude, longitude) {
    return `${latitude.toFixed(4)}, ${longitude.toFixed(4)}`;
}

function getCoordinateKey(latitude, longitude) {
    return `${latitude.toFixed(4)},${longitude.toFixed(4)}`;
}

function getCurrentLocationLabel() {
    const key = getCoordinateKey(appSettings.currentLatitude, appSettings.currentLongitude);
    if (appSettings.currentLocationName && appSettings.currentLocationKey === key) {
        return appSettings.currentLocationName;
    }

    return formatCoordinates(appSettings.currentLatitude, appSettings.currentLongitude);
}

function buildPlaceName(address = {}) {
    const primary = [
        address.neighbourhood,
        address.suburb,
        address.borough,
        address.city_district,
        address.town,
        address.village,
        address.hamlet,
        address.city,
        address.county,
        address.state,
    ].find(Boolean);

    if (!primary) {
        return null;
    }

    if (address.country_code === "us" && address.state) {
        const stateCode = address["ISO3166-2-lvl4"]?.split("-")[1];
        if (stateCode && primary !== address.state && !primary.includes(stateCode)) {
            return `${primary}, ${stateCode}`;
        }
    }

    return primary;
}

async function reverseGeocode(latitude, longitude) {
    const url = new URL("https://api.bigdatacloud.net/data/reverse-geocode-client");
    url.searchParams.set("latitude", latitude);
    url.searchParams.set("longitude", longitude);
    url.searchParams.set("localityLanguage", "en");

    const response = await fetch(url.toString(), {
        headers: {
            Accept: "application/json",
        },
    });

    if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
    }

    const payload = await response.json();
    return [
        payload.locality,
        payload.city,
        payload.principalSubdivision,
        payload.countryName,
    ].find(Boolean) || null;
}

async function resolveCurrentLocationName() {
    const latitude = appSettings.currentLatitude;
    const longitude = appSettings.currentLongitude;
    const key = getCoordinateKey(latitude, longitude);

    if (appSettings.currentLocationName && appSettings.currentLocationKey === key) {
        return;
    }

    try {
        const placeName = await reverseGeocode(latitude, longitude);
        if (!placeName) {
            return;
        }

        appSettings.currentLocationName = placeName;
        appSettings.currentLocationKey = key;

        const homeKey = getCoordinateKey(appSettings.homeLatitude, appSettings.homeLongitude);
        if (homeKey === key) {
            appSettings.homeLocationName = placeName;
            appSettings.homeLocationKey = key;
        }

        saveSettings();
        setFormFromSettings();
    } catch {
        // Keep coordinates as the fallback label when reverse geocoding fails.
    }
}

function prefersFahrenheit() {
    return false;
}

function weatherDescriptionFromCode(code) {
    const map = {
        0: "Clear",
        1: "Mostly clear",
        2: "Partly cloudy",
        3: "Overcast",
        45: "Fog",
        48: "Icy fog",
        51: "Light drizzle",
        53: "Drizzle",
        55: "Heavy drizzle",
        56: "Freezing drizzle",
        57: "Heavy freezing drizzle",
        61: "Light rain",
        63: "Rain",
        65: "Heavy rain",
        66: "Freezing rain",
        67: "Heavy freezing rain",
        71: "Light snow",
        73: "Snow",
        75: "Heavy snow",
        77: "Snow grains",
        80: "Rain showers",
        81: "Heavy showers",
        82: "Violent rain",
        85: "Snow showers",
        86: "Heavy snow showers",
        95: "Thunderstorm",
        96: "Storm with hail",
        99: "Severe storm",
    };

    return map[code] || "Weather";
}

function weatherThemeFromCurrent(current) {
    const { weather_code: code, is_day: isDay } = current;

    if ([95, 96, 99].includes(code)) {
        return "storm";
    }

    if ([71, 73, 75, 77, 85, 86].includes(code) || (current.snowfall || 0) > 0) {
        return isDay ? "snow-day" : "snow-night";
    }

    if (
        [51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82].includes(code) ||
        (current.rain || 0) > 0 ||
        (current.showers || 0) > 0
    ) {
        return isDay ? "rain-day" : "rain-night";
    }

    if ([1, 2, 3, 45, 48].includes(code) || (current.cloud_cover || 0) > 45) {
        return isDay ? "cloudy-day" : "cloudy-night";
    }

    return isDay ? "clear-day" : "clear-night";
}

function formatTemperature(value, unit) {
    return `${Math.round(value)}°${unit}`;
}

function formatClock(dateString) {
    return new Date(dateString).toLocaleTimeString([], {
        hour: "numeric",
        minute: "2-digit",
    });
}

function buildWeatherMeta(current, daily) {
    const parts = [current.is_day ? "Daytime outside" : "Night outside"];

    if (daily?.sunrise?.[0] && daily?.sunset?.[0]) {
        parts.push(`Sunrise ${formatClock(daily.sunrise[0])}`);
        parts.push(`Sunset ${formatClock(daily.sunset[0])}`);
    }

    if ((current.wind_speed_10m || 0) >= 12) {
        parts.push(`Wind ${Math.round(current.wind_speed_10m)} ${prefersFahrenheit() ? "mph" : "km/h"}`);
    }

    return parts.join(" • ");
}

function buildWeatherSummary(payload) {
    const current = payload.current;
    const unit = payload.current_units.temperature_2m === "°F" ? "F" : "C";
    return {
        theme: weatherThemeFromCurrent(current),
        temperatureText: formatTemperature(current.temperature_2m, unit),
        statusText: weatherDescriptionFromCode(current.weather_code),
        metaText: buildWeatherMeta(current, payload.daily),
    };
}

async function loadWeather() {
    if (!elements.heroCard) {
        return;
    }

    const latitude = appSettings.currentLatitude;
    const longitude = appSettings.currentLongitude;
    const tempUnit = prefersFahrenheit() ? "fahrenheit" : "celsius";
    const windUnit = prefersFahrenheit() ? "mph" : "kmh";

    try {
        const url = new URL("https://api.open-meteo.com/v1/forecast");
        url.searchParams.set("latitude", latitude);
        url.searchParams.set("longitude", longitude);
        url.searchParams.set(
            "current",
            "temperature_2m,weather_code,is_day,cloud_cover,rain,showers,snowfall,wind_speed_10m"
        );
        url.searchParams.set("daily", "sunrise,sunset");
        url.searchParams.set("forecast_days", "1");
        url.searchParams.set("timezone", "auto");
        url.searchParams.set("temperature_unit", tempUnit);
        url.searchParams.set("wind_speed_unit", windUnit);

        const response = await fetch(url.toString());
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        const payload = await response.json();
        setWeatherDisplay(buildWeatherSummary(payload));
    } catch {
        setWeatherDisplay({
            theme: elements.heroCard?.dataset.weatherTheme || "clear-day",
            temperatureText: "--",
            statusText: "Weather unavailable",
            metaText: "Could not match the outside conditions right now.",
        });
    }
}

function startWeatherRefresh() {
    if (weatherRefreshTimer) {
        clearInterval(weatherRefreshTimer);
    }

    void loadWeather();
    weatherRefreshTimer = window.setInterval(() => {
        void loadWeather();
    }, WEATHER_REFRESH_MS);
}

function updateInstallButtonVisibility() {
    if (elements.installButton) {
        elements.installButton.classList.toggle("hidden-panel", !deferredInstallPrompt);
    }
}

function parseTime24ToDate(time24) {
    const [hours, minutes] = time24.split(":").map(Number);
    const date = new Date();
    date.setHours(hours, minutes, 0, 0);
    return date;
}

function toDisplayTime(time24) {
    return parseTime24ToDate(time24).toLocaleTimeString([], {
        hour: "numeric",
        minute: "2-digit",
    });
}

function getCountdownText(targetDate) {
    const diffMs = targetDate.getTime() - Date.now();

    if (diffMs <= 0) {
        return "Now";
    }

    const totalMinutes = Math.floor(diffMs / 60000);
    const hours = Math.floor(totalMinutes / 60);
    const minutes = totalMinutes % 60;

    if (hours === 0) {
        return `in ${minutes}m`;
    }

    return `in ${hours}h ${minutes}m`;
}

function findNextPrayer(prayers) {
    const now = new Date();
    const nextPrayer = prayers.find((prayer) => parseTime24ToDate(prayer.time24) > now);

    if (nextPrayer) {
        return {
            prayer: nextPrayer,
            prayerDate: parseTime24ToDate(nextPrayer.time24),
        };
    }

    const tomorrowPrayerDate = parseTime24ToDate(prayers[0].time24);
    tomorrowPrayerDate.setDate(tomorrowPrayerDate.getDate() + 1);

    return {
        prayer: prayers[0],
        prayerDate: tomorrowPrayerDate,
    };
}

function renderPrayerList(prayers, activePrayerName) {
    if (!elements.prayerList) {
        return;
    }

    elements.prayerList.innerHTML = prayers
        .map((prayer) => {
            const activeClass = prayer.name === activePrayerName ? "active" : "";
            return `
                <div class="prayer-row ${activeClass}">
                    <span class="prayer-name">${prayer.name}</span>
                    <span class="prayer-time">${prayer.displayTime}</span>
                </div>
            `;
        })
        .join("");
}

function updateNextPrayerDisplay() {
    if (!currentSchedule || !elements.nextPrayerName || !elements.nextPrayerTime || !elements.countdownText) {
        return;
    }

    const { prayer: nextPrayer, prayerDate: nextPrayerDate } = findNextPrayer(currentSchedule.prayers);
    const countdown = getCountdownText(nextPrayerDate);

    elements.nextPrayerName.textContent = nextPrayer.name;
    elements.nextPrayerTime.textContent = nextPrayer.displayTime;
    elements.countdownText.textContent = countdown === "Now"
        ? `${nextPrayer.name} time`
        : `${nextPrayer.name} ${countdown}`;

    renderPrayerList(currentSchedule.prayers, nextPrayer.name);
}

function startCountdownTimer() {
    if (nextPrayerTimer) {
        clearInterval(nextPrayerTimer);
    }

    updateNextPrayerDisplay();
    nextPrayerTimer = window.setInterval(updateNextPrayerDisplay, 30000);
}

function scheduleBrowserReminder() {
    if (reminderTimeoutId) {
        clearTimeout(reminderTimeoutId);
        reminderTimeoutId = null;
    }

    if (!appSettings.notificationsEnabled || !currentSchedule || Notification.permission !== "granted") {
        return;
    }

    const { prayer: nextPrayer, prayerDate: nextPrayerDate } = findNextPrayer(currentSchedule.prayers);
    const delay = nextPrayerDate.getTime() - Date.now();

    if (delay <= 0 || lastReminderPrayer === `${currentSchedule.readableDate}:${nextPrayer.name}`) {
        return;
    }

    reminderTimeoutId = window.setTimeout(() => {
        new Notification("My Namaz", {
            body: `${nextPrayer.name} time is now.`,
        });
        lastReminderPrayer = `${currentSchedule.readableDate}:${nextPrayer.name}`;
        updateNextPrayerDisplay();
        scheduleBrowserReminder();
    }, delay);
}

async function enableNotificationsIfNeeded() {
    if (!appSettings.notificationsEnabled) {
        return true;
    }

    if (!("Notification" in window)) {
        setStatus("This browser does not support notifications.");
        if (elements.notificationsToggle) {
            elements.notificationsToggle.checked = false;
        }
        appSettings.notificationsEnabled = false;
        saveSettings();
        return false;
    }

    if (Notification.permission === "granted") {
        return true;
    }

    const permission = await Notification.requestPermission();

    if (permission !== "granted") {
        setStatus("Notification permission was not granted.");
        if (elements.notificationsToggle) {
            elements.notificationsToggle.checked = false;
        }
        appSettings.notificationsEnabled = false;
        saveSettings();
        return false;
    }

    return true;
}

function buildScheduleFromPayload(payload) {
    const timings = payload.data.timings;

    return {
        readableDate: payload.data.date.readable,
        prayers: PRAYER_ORDER.map((name) => {
            const time24 = timings[name].split(" ")[0];
            return {
                name,
                time24,
                displayTime: toDisplayTime(time24),
            };
        }),
    };
}

async function loadPrayerTimes() {
    const latitude = appSettings.currentLatitude;
    const longitude = appSettings.currentLongitude;
    saveSettings();

    setStatus("Loading prayer times...");

    try {
        const url = `https://api.aladhan.com/v1/timings?latitude=${latitude}&longitude=${longitude}&method=${ALADHAN_METHOD}`;
        const response = await fetch(url);

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        const payload = await response.json();
        currentSchedule = buildScheduleFromPayload(payload);
        setFormFromSettings();
        startCountdownTimer();
        void loadWeather();

        const notificationsReady = await enableNotificationsIfNeeded();
        if (notificationsReady) {
            scheduleBrowserReminder();
        }

        setStatus(`Loaded ${currentSchedule.readableDate} prayer times.`);
    } catch (error) {
        setStatus(`Could not load prayer times: ${error.message}`);
    }
}

function applyHomeCoordinates() {
    appSettings.currentLatitude = appSettings.homeLatitude;
    appSettings.currentLongitude = appSettings.homeLongitude;
    appSettings.currentLocationName = appSettings.homeLocationName;
    appSettings.currentLocationKey = appSettings.homeLocationKey;
    appSettings.locationSource = "Home coordinates";
    saveSettings();
    setFormFromSettings();
    setStatus("Home coordinates loaded.");
    void resolveCurrentLocationName();
    void loadWeather();
}

function saveCurrentAsHome() {
    appSettings.homeLatitude = appSettings.currentLatitude;
    appSettings.homeLongitude = appSettings.currentLongitude;
    appSettings.homeLocationName = appSettings.currentLocationName;
    appSettings.homeLocationKey = getCoordinateKey(
        appSettings.currentLatitude,
        appSettings.currentLongitude
    );
    appSettings.locationSource = "Home coordinates";
    saveSettings();
    setFormFromSettings();
    setStatus("Saved the current location as home.");
}

function requestDeviceLocation() {
    if (!navigator.geolocation) {
        setStatus("This browser does not support location.");
        return;
    }

    setStatus("Requesting location...");

    navigator.geolocation.getCurrentPosition(
        (position) => {
            appSettings.currentLatitude = position.coords.latitude;
            appSettings.currentLongitude = position.coords.longitude;
            appSettings.currentLocationName = null;
            appSettings.currentLocationKey = null;
            appSettings.locationSource = "Device location";
            saveSettings();
            setFormFromSettings();
            setStatus("Device location received. Refresh times to update.");
            void resolveCurrentLocationName();
            void loadWeather();
        },
        () => {
            setStatus("Location permission was denied or unavailable.");
        }
    );
}

function bindEvents() {
    if (elements.menuButton) {
        elements.menuButton.addEventListener("click", (event) => {
            event.preventDefault();
            setMenuOpen(!menuOpen);
        });
    }

    if (elements.settingsMenuItem) {
        elements.settingsMenuItem.addEventListener("click", () => {
            window.location.href = "./settings.html";
        });
    }

    if (elements.backButton) {
        elements.backButton.addEventListener("click", () => {
            window.location.href = "./index.html";
        });
    }

    document.addEventListener("click", (event) => {
        if (!menuOpen || !elements.menuDropdown || !elements.menuButton) {
            return;
        }

        const clickedInsideMenu = elements.menuDropdown.contains(event.target) || elements.menuButton.contains(event.target);
        if (!clickedInsideMenu) {
            setMenuOpen(false);
        }
    });

    if (elements.useLocationButton) {
        elements.useLocationButton.addEventListener("click", requestDeviceLocation);
    }
    if (elements.useSavedButton) {
        elements.useSavedButton.addEventListener("click", applyHomeCoordinates);
    }
    if (elements.saveHomeButton) {
        elements.saveHomeButton.addEventListener("click", saveCurrentAsHome);
    }
    if (elements.loadTimesButton) {
        elements.loadTimesButton.addEventListener("click", loadPrayerTimes);
    }

    if (elements.notificationsToggle) {
        elements.notificationsToggle.addEventListener("change", async () => {
            appSettings.notificationsEnabled = elements.notificationsToggle.checked;
            saveSettings();
            const notificationsReady = await enableNotificationsIfNeeded();

            if (notificationsReady) {
                scheduleBrowserReminder();
                setStatus(appSettings.notificationsEnabled
                    ? "Browser reminders enabled while this page stays open."
                    : "Browser reminders disabled.");
            }
        });
    }

    if (elements.installButton) {
        elements.installButton.addEventListener("click", async () => {
            if (!deferredInstallPrompt) {
                setInstallStatus("Use Chrome menu > Install app if the browser button is not ready yet.");
                return;
            }

            deferredInstallPrompt.prompt();
            const choice = await deferredInstallPrompt.userChoice;

            if (choice.outcome === "accepted") {
                setInstallStatus("App install accepted. Check your home screen.");
            } else {
                setInstallStatus("Install was dismissed. You can try again from the Chrome menu.");
            }

            deferredInstallPrompt = null;
            updateInstallButtonVisibility();
        });
    }
}

function bindPwaEvents() {
    window.addEventListener("beforeinstallprompt", (event) => {
        event.preventDefault();
        deferredInstallPrompt = event;
        updateInstallButtonVisibility();
        setInstallStatus("Install is ready. Tap Install App.");
    });

    window.addEventListener("appinstalled", () => {
        deferredInstallPrompt = null;
        updateInstallButtonVisibility();
        setInstallStatus("My Namaz is installed on this device.");
    });
}

async function registerServiceWorker() {
    if (!("serviceWorker" in navigator)) {
        setInstallStatus(
            isIosBrowser()
                ? "Open in Safari and use Add to Home Screen."
                : "Install from the browser menu. Offline support is limited here."
        );
        return;
    }

    try {
        await navigator.serviceWorker.register("./sw.js?v=5");
        setInstallStatus(
            isIosBrowser()
                ? "Open in Safari and use Add to Home Screen."
                : "Use Chrome menu > Install app, or tap Install App here."
        );
    } catch (error) {
        setInstallStatus(`PWA setup failed: ${error.message}`);
    }
}

async function initialize() {
    bindEvents();
    bindPwaEvents();
    setFormFromSettings();
    updateInstallButtonVisibility();
    void resolveCurrentLocationName();
    startWeatherRefresh();
    await registerServiceWorker();
    await loadPrayerTimes();
}

initialize();
