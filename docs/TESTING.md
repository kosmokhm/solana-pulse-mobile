# Testing and evidence

## Automated tests

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --console=plain
```

Results: `app/build/test-results/testDebugUnitTest/` (JUnit XML) and `app/build/reports/tests/testDebugUnitTest/` (HTML). Current finalization results are in [FINALIZATION_REPORT.md](../FINALIZATION_REPORT.md).

| Suite | Cases | Coverage |
| --- | ---: | --- |
| DemoLedgerTest | 25 | Reset, daily guard, duplicate mission, durable receipts, points/XP, Studio rewards |
| GeofencePolicyTest | 12 | Accuracy, conservative range boundary, stale/mock/invalid fixes |
| ReceiptPresentationTest | 3 | Receipt presentation |
| WalletSessionTest | 18 | Mock save/restore, cold start/process replacement, token update, revoked/unavailable wallet, changed account, disconnect and demo isolation |
| WalletSessionEncryptionTest | 8 | JVM AES-GCM, corruption/key failures, storage redaction |

These 66 JVM tests use mocks/fakes. They do not exercise an Android Keystore key, actual wallet transport, physical GPS or instrumented Activity recreation. No Android instrumented test suite is defined. Static app-logging review complements redaction tests; it cannot certify every third-party/system log.

## Earlier physical Seeker observations — October 9, 2026

These summarize private UI07 device/MWA reports, not fresh finalization runs. Device identifiers, wallet addresses, coordinate-bearing preferences, user-data hashes and personal diagnostics are excluded from publication.

| Scenario | Result | Boundary |
| --- | --- | --- |
| Upgrade with install -r | PASS | UI07 version verified; persisted demo preferences unchanged |
| Real wallet authorization | PASS | User manually authorized; Connected • MWA observed |
| Force-stop / cold start | PASS with interaction | New process restored after selecting the same wallet |
| Extra wallet consent on latest cold start | Not required in that run | User reported only selecting the wallet; Connected followed |
| Fully silent cold start | NOT ACHIEVED | Android chooser appeared in tested setup |
| HOME / resume | PASS | Connected without another wallet flow |
| Activity finish / relaunch | FAIL, one observed flow | Chooser/wallet flow stalled; Back returned to reconnect |
| Delayed chooser selection | FAIL reproduced | About 35 s delay crossed SDK failure around 30 s; late selection failed |
| Prompt fresh reconnect after cancellation | PASS | Immediate manual selection restored Connected without clearing data |
| Preferences across update/cancel/process death | PASS for unchanged file | Checksum equality shows preservation, not creation of every entity |
| Keystore integration | PASS for exercised restore path | No key/token exported; hardware backing and full migration NOT TESTED |
| Check-in creation / GPS / Studio during that MWA test | NOT TESTED | Prior development usage reported; no new end-to-end claim certification |
| Actual token rotation or revoked grant | NOT TESTED on device | Mock coverage; values intentionally not inspected |

## Safe manual regression

1. Check `adb devices -l`, package version and APK checksum. Use only compatible debug upgrades, preserving data.
2. Note existing visible points/XP and receipt state privately. Do not export preferences or session contents.
3. Authorize manually, without signing/transactions. Cold start and promptly select the same wallet if chooser appears; note whether extra consent was needed.
4. Compare state after HOME/resume and force-stop/cold start. Bound wallet attempts; use Back and deliberate reconnect if stalled. Avoid competing attempts.
5. Only with reset permission, confirm Reset Demo Data: expect 0 points/0 XP, no receipt/test zone/memes, wallet unchanged.
6. Explicitly create a zone; test permitted/denied GPS and accuracy/out-of-range states where safe. Claim once, note reward/time, then navigate/restart/remove zone: repeat claim stays blocked.
7. Verify daily repeat is blocked; share one Studio meme and check reward/XP delta. Do not alter clock or location settings without permission.

Finalization's ADB check listed no devices. No new real-device test or screenshot was performed. No phone clear/reset, wallet revocation or financial operation is part of this finalization.
