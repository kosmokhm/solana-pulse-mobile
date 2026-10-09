# Solana Pulse Mobile

A native Android hackathon demo combining real Solana wallet authorization, a location-based check-in, and a local creative rewards experience. Built for the Solana Mobile CLOCK IN Hackathon, October 12, 2026.

**UI07 · version 1.0-ui07 (code 6).** Kotlin and Jetpack Compose, previously tested on a physical Solana Seeker.

## What you can try

- **Explore:** real Mobile Wallet Adapter authorization, an explicitly user-created private 100 m test zone, GPS accuracy/range, one demo mission claim and a local receipt.
- **Rewards:** local Pulse Points, XP, tier progress and a once-per-UTC-day bonus.
- **SocialFi:** clearly labeled sample stories and memes created on this device; no live feed.
- **Studio:** templates, captions, preview and local meme sharing with local rewards.

**Pulse Points are local demo counters, not Solana tokens, SKR, money or on-chain rewards.** UI07 does not mint NFTs, transfer funds, submit transactions or request message signatures. The radar is schematic, not a geographic map. Local duplicate protection is not server-side proof of presence.

## Screenshots and video

Reviewed publication screenshots have not yet been added. Existing development captures contain user-specific information and are deliberately excluded from Git. No mockup is presented as a device screenshot. The finalization ADB check listed no connected devices.

See [media](media/README.md), the [shot list](docs/DEMO_SHOT_LIST.md) and the [2–3 minute video script](docs/DEMO_VIDEO_SCRIPT.md) for the planned Seeker captures.

## Build and run

Use Android Studio, Android SDK Platform 37 and the checked-in Gradle wrapper. Finalization used Android Studio JBR 25.0.3; Java/Kotlin bytecode targets 17. A first build needs dependency downloads. No API key or signing secret is required for the debug demo.

```powershell
git clone https://github.com/kosmokhm/solana-pulse-mobile.git
cd solana-pulse-mobile
# Configure your local Android SDK in Android Studio or untracked local.properties.
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
adb devices -l
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.pulse.mobile/.MainActivity
```

Install a compatible MWA wallet and manually approve authorization on the phone. An upgrade requires a compatible signing certificate; do not uninstall or clear data to work around a mismatch. See [setup](docs/SETUP.md).

## Technology

Kotlin · Jetpack Compose / Material 3 · AndroidX lifecycle · MWA clientlib-ktx 2.2.0 · Google Play Services Location 21.0.1 · Android Keystore AES-GCM · SharedPreferences · JUnit 4. There is no application backend.

See [architecture](docs/ARCHITECTURE.md), [features](docs/FEATURES.md), [testing](docs/TESTING.md), [security](docs/SECURITY.md) and [finalization results](FINALIZATION_REPORT.md).

## Honest limits

A successful physical cold start restored Connected after manual wallet selection, without a new approval prompt. Fully invisible cold-start restoration is not established. One Activity finish/relaunch MWA flow failed; delayed wallet selection also reproduced a stalled flow. These are documented in [known limitations](docs/KNOWN_LIMITATIONS.md).

## Hackathon materials

- [Submission summary](docs/HACKATHON_SUBMISSION.md)
- [Demo video script](docs/DEMO_VIDEO_SCRIPT.md)
- [Seeker shot list](docs/DEMO_SHOT_LIST.md)

No LICENSE file is currently included; the licensing choice remains with the repository owner.
