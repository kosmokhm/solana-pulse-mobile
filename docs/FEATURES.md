# Features — UI07

| Tab | Implemented | Boundary |
| --- | --- | --- |
| Explore | Real MWA, restoration states, wallet metadata, points dashboard, schematic radar, GPS accuracy/distance/range, test zone, claim and receipt | No street map, deployed hotspots, server presence proof or blockchain receipt |
| Rewards | Local points/XP, tiers, activity counters, daily bonus | No token balance, staking or withdrawal |
| SocialFi | Sample stories, scrolling filters, transient likes and local memes | No live users/backend; categories are not network rankings |
| Studio | Six templates, captions, preview, local sharing and rewards | No upload or NFT/cNFT mint |

Initial/reset state: 0 Pulse Points and 0 XP. Check-in and Studio each use base reward 50 points, modified by the existing current-tier calculation. Check-in adds 25 XP; Studio adds 15 XP. Daily claim adds 20 points once per UTC date, without XP.

| Tier | XP threshold | Multiplier |
| --- | ---: | ---: |
| Bronze | 0 | 1.00× |
| Silver | 100 | 1.25× |
| Gold | 300 | 1.50× |
| Diamond | 1000 | 2.00× |

The pre-award tier determines the existing integer reward calculation. One demo mission stays claimed after deleting its zone. Confirmed debug Reset Demo Data is the supported repeat-test mechanism; it does not clear wallet authorization.

The actual Solana integration is authorization with an installed wallet. The adapter retains its existing Devnet configuration; no RPC-backed balance or asset feature is implemented. Rewards, progression, receipts and meme sharing are local. Points are not SKR, tokens, money or on-chain assets.
