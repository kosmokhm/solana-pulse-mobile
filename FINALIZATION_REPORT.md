# UI07 clean public preparation report

## Scope and provenance

This independent source copy was exported from reviewed UI07 commit `b2b8b0fed463dda4ab9cc9c1026e8d76fd74df7e` using its 59-file allowlist. It does not inherit the original Git history, reflog, dangling objects, caches, private reports, diagnostics, APKs or archives. Application and build files are byte-identical to that source commit. Only this report and publication-provenance wording in SETUP, SECURITY and KNOWN_LIMITATIONS were updated.

A complete private backup of the original workspace, including .git, was verified outside this repository: 2,227 files, 238,302,575 bytes. Every file passed SHA-256 comparison; a full restore rehearsal and Git fsck passed. Backup ACLs restrict access to the current Windows user, SYSTEM and Administrators. The private backup location is intentionally not published here. The original workspace was rehashed after the new build and remained unchanged.

## Clean Git preparation

Branch: `master`. One independent initial commit is created after the final staged audit, using the owner's approved GitHub noreply identity. No remote is configured. No push, network upload, release, deployment, old-history rewrite or GitHub setting change is performed.

The new history contains only reviewed source, tests, wrapper and documentation. Before commit, all 59 candidate files passed the credential/personal-path and size scan; none exceeds 1 MiB. The only intended binary is the pre-existing Gradle wrapper JAR. Post-commit verification checks one root commit, no parent history, no remote/tags, clean status and the complete reachable object set. The resulting commit hash is delivered separately to the owner, as a commit cannot contain its own hash.

The original repository and existing GitHub repository remain separate. Publishing this clean root over an existing branch is not a fast-forward update and requires a separate owner-approved plan. This preparation does not solve or change remote historical exposure.

## Application identity and validation

- Application ID/namespace: `com.pulse.mobile`; launcher: `com.pulse.mobile.MainActivity`.
- Version: `1.0-ui07`, versionCode `6`; min/target/compile SDK: 24/34/37.
- Existing toolchain: Gradle 9.7.1, AGP 9.4.0, Kotlin/Compose plugin 2.1.0; Android Studio JBR 25.0.3 with Java/Kotlin target 17.
- Build command: `gradlew.bat :app:assembleDebug :app:testDebugUnitTest --offline --no-daemon --console=plain`, with a temporary project cache and SDK configuration supplied only through the process environment.
- **PASS: BUILD SUCCESSFUL in 2m 32s; 44 tasks executed.**
- **PASS: 66 JVM tests, 0 failures, 0 errors, 0 skipped** across all five existing suites.
- Reports/build output stay ignored; the complete console log is outside this repository in private storage. No local.properties was introduced.
- Existing AGP deprecation/native-library warnings remain. No clean, release signing or dependency upgrade was performed.
- No fresh physical Seeker/MWA test was performed in this task. Earlier real observations and the MWA lifecycle FAIL remain honestly documented in TESTING and KNOWN_LIMITATIONS.

## Local debug artifact

Path: `artifacts/PulseMobile-UI07-final-debug.apk` (ignored, not in Git).

Size: 12,319,964 bytes. Build-output timestamp: 2026-10-09T23:00:29.917671+03:00.

SHA-256: `2af620289aa3dad0d61d14cc885f1fae9dfd5d2d314d23f1f2c034e781b75e60`.

This is a debug test artifact, not a release build. APK or video hosting requires a separate reviewed decision. No artifact from the original workspace was copied into the public source tree.

## Security and publication limits

Credentials, private keys, seed phrases, authorization tokens, credential-bearing URLs and personal local-path patterns were checked without printing any potential secret values. Pattern scans reduce risk; they are not a complete external security assessment or proof of the absence of every possible secret. The original private history is deliberately excluded.

README and docs describe Pulse Points/XP as local demo state. No implemented SKR bonus, token reward, mint, transfer, message signing, live backend or geographic tile map is claimed. The known Android chooser and lifecycle stall remain documented; invisible cold-start restoration is not promised. Local claim guards are not server anti-fraud.

No LICENSE was added. The owner must choose licensing separately. Media contains guidance only; real, privacy-approved Seeker screenshots and a 2?3 minute video are still pending. Private screenshots/diagnostics are excluded. Existing source EOF whitespace was preserved rather than modifying UI07.

## Source publication manifest

Exactly 59 files are intended for the initial source commit:

- `.gitignore`
- `FINALIZATION_REPORT.md`
- `README.md`
- `app/.gitignore`
- `app/build.gradle.kts`
- `app/src/debug/kotlin/com/pulse/mobile/ui/debug/DeveloperDemoTools.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/kotlin/com/pulse/mobile/MainActivity.kt`
- `app/src/main/kotlin/com/pulse/mobile/data/DataPlaceholder.kt`
- `app/src/main/kotlin/com/pulse/mobile/data/DemoMemeRepository.kt`
- `app/src/main/kotlin/com/pulse/mobile/data/GeofencePolicy.kt`
- `app/src/main/kotlin/com/pulse/mobile/data/HotspotConfig.kt`
- `app/src/main/kotlin/com/pulse/mobile/data/LocationHelper.kt`
- `app/src/main/kotlin/com/pulse/mobile/data/TokenManager.kt`
- `app/src/main/kotlin/com/pulse/mobile/domain/DomainPlaceholder.kt`
- `app/src/main/kotlin/com/pulse/mobile/mwa/AndroidWalletSessionStore.kt`
- `app/src/main/kotlin/com/pulse/mobile/mwa/EncryptedWalletSessionStore.kt`
- `app/src/main/kotlin/com/pulse/mobile/mwa/WalletConnectionManager.kt`
- `app/src/main/kotlin/com/pulse/mobile/mwa/WalletSession.kt`
- `app/src/main/kotlin/com/pulse/mobile/mwa/WalletSessionController.kt`
- `app/src/main/kotlin/com/pulse/mobile/mwa/WalletSessionViewModel.kt`
- `app/src/main/kotlin/com/pulse/mobile/ui/MainScreen.kt`
- `app/src/main/kotlin/com/pulse/mobile/ui/PulseComponents.kt`
- `app/src/main/kotlin/com/pulse/mobile/ui/PulseTheme.kt`
- `app/src/main/kotlin/com/pulse/mobile/ui/ReceiptPresentation.kt`
- `app/src/main/kotlin/com/pulse/mobile/ui/feed/SocialFeedScreen.kt`
- `app/src/main/kotlin/com/pulse/mobile/ui/map/CheckInScreen.kt`
- `app/src/main/kotlin/com/pulse/mobile/ui/map/MapScreen.kt`
- `app/src/main/kotlin/com/pulse/mobile/ui/meme/MemeEditorScreen.kt`
- `app/src/main/kotlin/com/pulse/mobile/ui/rewards/RewardsScreen.kt`
- `app/src/main/res/values/themes.xml`
- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`
- `app/src/release/kotlin/com/pulse/mobile/ui/debug/DeveloperDemoTools.kt`
- `app/src/test/kotlin/com/pulse/mobile/data/DemoLedgerTest.kt`
- `app/src/test/kotlin/com/pulse/mobile/data/GeofencePolicyTest.kt`
- `app/src/test/kotlin/com/pulse/mobile/mwa/WalletSessionEncryptionTest.kt`
- `app/src/test/kotlin/com/pulse/mobile/mwa/WalletSessionTest.kt`
- `app/src/test/kotlin/com/pulse/mobile/ui/ReceiptPresentationTest.kt`
- `build.gradle.kts`
- `docs/ARCHITECTURE.md`
- `docs/DEMO_SHOT_LIST.md`
- `docs/DEMO_VIDEO_SCRIPT.md`
- `docs/FEATURES.md`
- `docs/HACKATHON_SUBMISSION.md`
- `docs/KNOWN_LIMITATIONS.md`
- `docs/SECURITY.md`
- `docs/SETUP.md`
- `docs/TESTING.md`
- `gradle.properties`
- `gradle/wrapper/gradle-wrapper.jar`
- `gradle/wrapper/gradle-wrapper.properties`
- `gradlew`
- `gradlew.bat`
- `media/README.md`
- `media/screenshots/.gitkeep`
- `media/video/.gitkeep`
- `media/video/README.md`
- `settings.gradle.kts`
