# Solana Pulse Mobile — CLOCK IN submission draft

**Platform:** native Android, Kotlin/Jetpack Compose. **Baseline:** UI07, 1.0-ui07 (6). **Hackathon target:** October 12, 2026.

## Problem

Mobile community experiences need a clear connection between wallet identity, physical participation and creative contribution. A demo should illustrate that interaction without presenting local counters as tokens or GPS as decentralized proof.

## Solution

PulseMobile combines a wallet-aware location check-in and a local creative rewards loop. Explore presents a schematic radar and a user-created zone; Rewards explains progression; SocialFi separates sample stories from on-device creations; Studio creates captioned memes.

## Solana Mobile integration

Real Mobile Wallet Adapter authorization with an installed wallet has been exercised on a physical Solana Seeker. UI07 encrypts the grant with Android Keystore and confirms the session through MWA after a cold start. The latest observed restore needed provider selection without another approval prompt. A known stalled lifecycle flow remains documented.

No transaction, NFT mint, SKR payout or message signature occurs. Points are local. Real integrations are wallet authorization and device location; rewards and community stories are explicitly demo behavior.

## Jury demo

1. Show native Explore and real Connected • MWA.
2. Explain local points and schematic radar.
3. Explicitly create a 100 m test zone; inspect GPS accuracy/range.
4. Claim once; show receipt reward/time and blocked repeat claim.
5. Show Rewards and daily guard.
6. Make a Studio meme and find it in SocialFi beside labeled sample content.
7. Relaunch; select the same provider if prompted and show confirmed restoration.

Use debug reset only with permission. Do not promise every GPS environment or wallet lifecycle path will succeed. Read [limitations](KNOWN_LIMITATIONS.md) before presenting.

## Strengths and remaining submission work

Native Seeker-oriented UI, real MWA authorization, encrypted grant persistence, accuracy-aware GPS gating, durable local claim receipts, clear demo boundaries and mock-based automated tests with honestly reported device observations.

Owner tasks: approve GitHub visibility/branch and historical-data handling, capture privacy-reviewed screenshots/video, complete team attribution/form fields and decide licensing. No submission or deployment has been performed by this task.
