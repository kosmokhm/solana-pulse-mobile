# Security and privacy — demo threat model

## Wallet boundary

The installed wallet retains Solana private keys. PulseMobile uses MWA authorization only; no private keys, seeds, signed transactions or message signatures are collected. Current features have no transfer/mint operation. A cached public address is not authentication without successful MWA in the current process.

An authorization token is a bearer credential. UI07 encrypts the token/binding metadata with AES-256-GCM, an Android Keystore key and an atomic file under `noBackupFilesDir`. Random nonces and authentication protect storage. Android backup is disabled and explicit rules exclude demo/auth stores. Hardware backing/attestation and compromised-device resistance are not claimed.

Restoration supplies the saved token to the official SDK; fresh returned metadata is saved before Connected. A storage failure prevents false Connected. Account/provider changes require confirmation. Ordinary app closure is not Disconnect. Explicit Disconnect locally disables restore and attempts supported revocation, which may fail when the wallet is unavailable.

Never log/display a token, add it to crash payloads or publish it. Error handling avoids raw SDK exception bodies. Tests use synthetic fixtures, never real grants. See [official Android MWA guidance](https://docs.solanamobile.com/android-native/using_mobile_wallet_adapter).

## Location and local ledger

A test center is created only after explicit action and permission, stored privately, and not sent to analytics, Git, logs or on-chain. A receipt is local reward/time evidence, not a transaction. Points/XP are independent local counters.

Atomic preferences/claim flags protect normal repeat taps/restarts, not a malicious device owner, data removal, root or clock manipulation. Debug Reset is a confirmed demo-only action; wallet storage and other apps are untouched.

## Source publication

Root `.gitignore` excludes caches, IDE files, local SDK config, APKs/archives/videos, signing material, environment secrets, personal reports and probes. Ignore rules cannot remove tracked files or old history. This copy was created from an allowlisted source tree, without the original .git directory or private workspace artifacts.

Review staged content for secrets and unwanted binaries before publication. Never upload authorization grants, coordinate-bearing preferences, personal screenshots/logs or signing keys. Distributing debug APK/video requires a separate size/privacy decision; a GitHub Release asset can avoid large binaries in source history.

The original workspace and existing GitHub repository have separate historical-data risks; that history is not part of this clean copy. This copy has no configured remote. No history rewrite, credential revocation or push was performed. Owner approval and a plan for the existing remote history are required before publication. Pattern scanning is not proof that every possible credential is absent.

There is no external security audit, backend authorization service or production anti-fraud claim. Report suspected secrets privately to the owner; never paste them in public issues. Stop publication and coordinate remediation if a real credential is found.
