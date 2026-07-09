Android app scaffold for the Namaz Reminder rewrite.

Current scope:
- Kotlin Android app in a separate `android-app/` folder
- Simple single-activity flow matching the web preview
- Prayer fetching, saved home coordinates, device location path, local notifications, and widget support scaffolding

Block status:
- Block 1: scaffolded, but not yet verified in Android Studio
- Block 2 onward: code scaffolded locally, still needs Android Studio sync/build verification

Notes:
- This environment does not have `gradle`, so the Gradle wrapper was not generated here.
- Open this folder in Android Studio and let it sync with the Android Gradle Plugin versions in the build files.
- If Android Studio asks to create Gradle wrapper files, accept it.
