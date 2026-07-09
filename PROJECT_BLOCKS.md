Namaz Reminder block mapping

Block 1: Android project setup
- Status: scaffolded locally
- Pending: Android Studio sync, build, run, and visual verification
- Pending checklist file: `android-app/BLOCK1_PENDING.md` (ignored locally)

Block 2: Prayer-time logic
- Web: implemented in `web/app.js`
- Android: implemented in `android-app/app/src/main/java/com/hussain/namazreminder/prayer/`

Block 3: Phone location support
- Web: browser geolocation path implemented
- Android: device location helper scaffolded in `android-app/app/src/main/java/com/hussain/namazreminder/location/`

Block 4: Main app screen
- Web: complete mobile-style preview in `web/index.html` and `web/styles.css`
- Android: single-activity UI scaffolded in `android-app/app/src/main/res/layout/activity_main.xml`

Block 5: Local notifications
- Web: browser notifications while the page is open
- Android: notification channel, alarm receiver, and scheduler scaffolded in `android-app/app/src/main/java/com/hussain/namazreminder/notification/`

Block 6: Daily refresh logic
- Web: refreshes on demand and recomputes next prayer live
- Android: still needs runtime verification in Android Studio for final behavior

Block 7: Home screen widget
- Web: widget-style preview card implemented
- Android: app widget provider and layout scaffolded in `android-app/app/src/main/java/com/hussain/namazreminder/widget/`

Block 8: Connect widget to app data
- Web: widget preview uses the same loaded schedule data
- Android: widget updater is wired to the same next-prayer output in `MainActivity`

Block 9: Real-device testing
- Pending: requires Android Studio and your Samsung phone

Block 10: APK packaging
- Pending: requires Android Studio or Gradle wrapper generation
