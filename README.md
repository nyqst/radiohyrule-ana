# Radio Hyrule Mobile App for Android

- Platform: Android 4.1+ (API levels 16+); main focus is on handheld devices with small screens for now
- Version: pre-alpha

# Build Instructions

- Install the [Android Developer Tools](https://developer.android.com/tools/index.html)
- Using the SDK Manager, install at least the following packages (the revision in parentheses has been tested):
    - Tools / Android SDK Tools (rev. 23.0.2)
    - Tools / Android SDK Platform-tools (rev. 20)
    - Tools / Android SDK Build-tools (rev. 19.1)
    - Android 4.4.2 (API 19) / SDK Platform (rev. 3)
    - Extras / Android Support Repository (rev. 6)
    - Extras / Android Support Library (rev. 20)
- Run the `gradlew` script to set up gradle
- To build
    - Either run `gradlew assemble` on command line (see `gradlew tasks`),
    - Or import the project into your favorite IDE (as long as it is gradle wrapper-aware), and build from there