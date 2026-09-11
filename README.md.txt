Pulse Mobile ($SKR Super-App for Solana Mobile)

Pulse Mobile is an advanced Android-native Super-App built specifically for the Solana Mobile ecosystem (optimized for Solana Seeker and Android devices). It bridges DePIN geolocation check-ins, a gamified `$SKR` token economy, and a real-time SocialFi community feed, secured via Solana Mobile Wallet Adapter (MWA).

---

🚀 Key Features

* **DePIN Geolocation Check-Ins (`MapScreen.kt` & `CheckInScreen.kt`):** Interactive radar-style map featuring animated location pulses, hotspot markers, distance carousels, and GPS verification via Google Play Services Fused Location Provider.
* **`$SKR` Token Economy & Seeker Tiers (`TokenManager.kt` & `RewardsScreen.kt`):** Dynamic reward calculation based on user tiers (`BRONZE`, `SILVER`, `GOLD`, `DIAMOND`) and hotspot types (`STANDARD`, `SPONSORED`, `EPIC_EVENT`). Includes automated XP progression, level-ups, and daily streak bonuses.
* **SocialFi Activity Feed (`SocialFeedScreen.kt`):** Real-time activity stream showcasing community check-ins across the Solana Mobile network, featuring peer-to-peer likes and `$SKR` boost tipping.
* **Solana Mobile Integration (`MainScreen.kt`):** Native Web3 authentication framework ready for Mobile Wallet Adapter (MWA) and Seed Vault hardware security.

---

🛠️ Tech Stack

* **Language:** Kotlin
* **UI Framework:** Jetpack Compose & Material 3
* **Web3 Integration:** Solana Mobile Wallet Adapter SDK (`mobile-wallet-adapter-clientlib-ktx`)
* **Location Services:** Google Play Services Location (`FusedLocationProviderClient`)
* **Architecture:** Single-Activity Jetpack Compose Navigation with reactive state management

---

📦 Getting Started & Installation

Prerequisites

* Android Studio (Ladybug or newer recommended)
* Android SDK (Compile SDK 37, Min SDK 26+)
* Physical Solana Mobile device (e.g., Solana Seeker) or Android Emulator with developer options enabled.

Building from Source

1. Clone the repository and open the project root folder `PulseMobile` in Android Studio.
2. Allow Gradle to sync and resolve dependencies.
3. Build the debug APK via terminal or Android Studio:
```powershell
.\gradlew.bat assembleDebug

```


4. The generated APK will be available at:
`app/build/outputs/apk/debug/app-debug.apk`

### Installing on Device (ADB)

Connect your Solana Seeker via USB, enable USB Debugging, and install the package using ADB:

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk

```

---

📱 Demo Script for Judges (60 Seconds)

1. **0:00 - 0:10 | Initialization & Wallet:** Launch **Pulse Mobile** on the Solana Seeker device. Tap the top-right wallet button to simulate secure MWA authentication and view the connected public key.
2. **0:10 - 0:25 | DePIN Map & Check-In:** Navigate to the **Map** tab. Explore the radar animation and hotspot pins. Select a sponsored location and perform a GPS-verified check-in to trigger reward distributions and level-up popups.
3. **0:25 - 0:40 | Rewards & Seeker Tiers:** Switch to the **Rewards** tab. Review the Solana-style gradient balance card, active multiplier breakdown, and XP progress toward the next tier.
4. **0:40 - 0:55 | SocialFi Feed:** Open the **SocialFi** tab. Scroll through live check-ins from other community members, drop a like, and send an instant `$SKR` boost tip.
5. **0:55 - 1:00 | Conclusion:** Showcase the seamless performance on Solana Seeker, highlighting its readiness for the DePIN and Mobile ecosystem.