const STORAGE_KEY = "namaz-reminder-web-settings";
const ALADHAN_METHOD = "2";
const DEFAULT_SETTINGS = {
    homeLatitude: 40.9364,
    homeLongitude: -74.1767,
    currentLatitude: 40.9364,
    currentLongitude: -74.1767,
    locationSource: "Home coordinates",
    notificationsEnabled: false,
};

const PRAYER_ORDER = ["Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"];

const elements = {
    todayLabel: document.getElementById("todayLabel"),
    nextPrayerName: document.getElementById("nextPrayerName"),
    nextPrayerTime: document.getElementById("nextPrayerTime"),
    countdownText: document.getElementById("countdownText"),
    widgetPrayerName: document.getElementById("widgetPrayerName"),
    widgetPrayerTime: document.getElementById("widgetPrayerTime"),
    widgetCountdown: document.getElementById("widgetCountdown"),
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
    elements.statusText.textContent = message;
}

function setFormFromSettings() {
    elements.notificationsToggle.checked = appSettings.notificationsEnabled;
    const locationText = `${appSettings.locationSource} • ${appSettings.currentLatitude.toFixed(4)}, ${appSettings.currentLongitude.toFixed(4)}`;
    elements.locationSummary.textContent = locationText;
    elements.settingsLocationSummary.textContent = locationText;
    elements.todayLabel.textContent = new Date().toLocaleDateString([], {
        month: "short",
        day: "numeric",
    });
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

function updateNextPrayerDisplay() {
    if (!currentSchedule) {
        return;
    }

    const { prayer: nextPrayer, prayerDate: nextPrayerDate } = findNextPrayer(currentSchedule.prayers);
    const countdown = getCountdownText(nextPrayerDate);

    elements.nextPrayerName.textContent = nextPrayer.name;
    elements.nextPrayerTime.textContent = nextPrayer.displayTime;
    elements.countdownText.textContent = countdown === "Now"
        ? `${nextPrayer.name} time`
        : `${nextPrayer.name} ${countdown}`;

    elements.widgetPrayerName.textContent = nextPrayer.name;
    elements.widgetPrayerTime.textContent = nextPrayer.displayTime;
    elements.widgetCountdown.textContent = countdown === "Now"
        ? "Time now"
        : countdown;

    renderPrayerList(currentSchedule.prayers, nextPrayer.name);
}

function renderPrayerList(prayers, activePrayerName) {
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
        new Notification("Namaz Reminder", {
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
        elements.notificationsToggle.checked = false;
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
        elements.notificationsToggle.checked = false;
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
    appSettings.locationSource = "Home coordinates";
    saveSettings();
    setFormFromSettings();
    setStatus("Home coordinates loaded.");
}

function saveCurrentAsHome() {
    appSettings.homeLatitude = appSettings.currentLatitude;
    appSettings.homeLongitude = appSettings.currentLongitude;
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
            appSettings.locationSource = "Device location";
            saveSettings();
            setFormFromSettings();
            setStatus("Device location received. Load prayer times to refresh.");
        },
        () => {
            setStatus("Location permission was denied or unavailable.");
        }
    );
}

function bindEvents() {
    elements.useLocationButton.addEventListener("click", requestDeviceLocation);
    elements.useSavedButton.addEventListener("click", applyHomeCoordinates);
    elements.saveHomeButton.addEventListener("click", saveCurrentAsHome);
    elements.loadTimesButton.addEventListener("click", loadPrayerTimes);
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

async function initialize() {
    bindEvents();
    setFormFromSettings();
    await loadPrayerTimes();
}

initialize();
