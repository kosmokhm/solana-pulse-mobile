# Developer setup

## Requirements

- Android Studio or equivalent Android environment, SDK Platform 37, platform-tools and the project's resolved build tools.
- Android Studio bundled JBR; finalization used JBR 25.0.3. Bytecode targets Java 17.
- Existing pins: Gradle 9.7.1, Android Gradle Plugin 9.4.0, Kotlin/Compose plugin 2.1.0. These are project versions, not an upgrade recommendation.
- For real MWA/GPS: a physical Android device, compatible installed wallet, Google Play services and precise location permission. Minimum SDK 24, target SDK 34. Seeker was used previously.

## Configure and build

Open the root in Android Studio and select your SDK. Alternatively create untracked `local.properties` with `sdk.dir` pointing to your SDK; use forward slashes on Windows. No map API key or release signing secret is needed.

```powershell
$env:JAVA_HOME = 'C:/Program Files/Android/Android Studio/jbr'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --console=plain
```

The first build requires network access to dependency repositories. Offline mode needs a populated cache. Do not run `clean` to finalize this workspace. Packaging may replace files in the output directory, so first preserve older named APKs elsewhere.

## Install a debug upgrade

```powershell
adb devices -l
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.pulse.mobile/.MainActivity
```

Require device status `device`. An upgrade needs a compatible signature/version relationship. If signatures differ, stop and obtain a compatible build; do not uninstall or clear user data. The preserved finalization artifact is `artifacts/PulseMobile-UI07-final-debug.apk`, excluded from Git. See the finalization report for identity/checksum/time. No release signing configuration is created.

## First demo

Connect through the wallet card; manually approve only MWA authorization. Reject unexpected signing/transaction requests. Explicitly create a test hotspot at your position, grant precise location, inspect accuracy/range and claim once. Review the receipt/Rewards, then create a local Studio meme and inspect SocialFi.

Only with permission, confirmed debug Demo Reset prepares 0 points/0 XP without clearing wallet authorization. Never reset someone's installation merely to make a recording.

Keep APKs, videos, local configuration, diagnostics and personal screenshots out of Git. This clean preparation copy has no remote configured. The intended future repository is kosmokhm/solana-pulse-mobile on master, but its existing history requires a separate owner-approved publication plan. No push, force push, visibility change or old-history rewrite is part of preparation.
