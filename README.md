# Transfer Rate

AED-based rate aggregator for UAE remittance services, with per-country
gold and silver rates. Free to use, ad-free, no analytics, no accounts.

Proprietary software — all rights reserved. This repository is published
for transparency only; it is **not** open source and grants no licence to
use, copy, or redistribute the code (see [`LICENSE`](LICENSE)).

> **Latest release: [v0.37.0](https://github.com/imraneggy/transfer-rate/releases/tag/v0.37.0)**
> See [`CHANGELOG.md`](CHANGELOG.md) for the full version history;
> the rendered HTML report lives at [`docs/CHANGELOG.html`](docs/CHANGELOG.html).
> End-user docs are in [`docs/USER_GUIDE.md`](docs/USER_GUIDE.md).
>
> ⚠️ **Do not sideload v0.28.0 → v0.29.0** — those releases ship with a
> Compose `painterResource()` regression that crashes the app on
> launch. Fixed in v0.29.1. Older v0.27.x APKs are debug-signed (a
> separate security issue resolved in v0.28.1+); upgrade is one-way
> via uninstall + reinstall.

## What this is

A small Android 14+ app (~3.4 MB universal APK, **zero native
dependencies as of v0.29.x**) that shows three things side-by-side,
refreshed every 15 minutes:

1. **AED → INR remittance rates** from up to thirteen UAE money-transfer
   providers (Wise, Aspora, Remitly, TransferGo, Al Ansari, Al Dahab,
   Ahalia, Federal Exchange, GCC Exchange, Index Exchange, Orient Exchange,
   Lari, Sharaf Exchange). Providers without a verified public scraper stay
   out of the live provider list until they can be sourced reliably.
2. **The Google Finance mid-market rate** as a benchmark, so the
   provider spread is visible at a glance.
3. **Gold & silver rates** for the UAE (Khaleej Times) and India
   (LiveChennai), with 24K + 22K gold (1 g + 8 g) and silver (1 g +
   1 kg), plus 30-day history.

## Tech stack

| Layer | Component | Version |
|-------|-----------|---------|
| Android — runtime | minSdk / targetSdk | **34** (Android 14) |
| Android — runtime | compileSdk | **35** |
| Android — runtime | JDK | **17** |
| Android — language | Kotlin | **2.1.0** |
| Android — toolchain | Android Gradle Plugin | **8.7.3** |
| Android — UI | Compose BOM | **2024.12.01** |
| Android — UI | Material 3 (`compose-material3`) | (BOM-managed) |
| Android — UI | Activity Compose | **1.9.3** |
| Android — UI | Lifecycle ViewModel + Runtime | **2.8.7** |
| Android — networking | OkHttp | **4.12.0** |
| Android — serialization | kotlinx.serialization JSON | **1.7.3** |
| Android — concurrency | kotlinx.coroutines (Android) | **1.9.0** |
| Android — background | WorkManager (`work-runtime-ktx`) | **2.10.0** |
| Android — fonts | Manrope, Space Grotesk (OFL 1.1) | bundled |
| Data pipeline | Python (scrapers + orchestrator) | **3.11+** |
| Data pipeline | GitHub Actions (cron `*/15`) | hosted |
| Data hosting | GitHub Pages (Fastly CDN) | static |
| Refresh proxy | Cloudflare Worker (free tier) | hosted |
| Distribution | Google Play (release-signed APK) | per-ABI splits + universal |

> **Note on ABI splits.** From v0.29.x onward the app has zero native
> dependencies (no `.so` files), so all ABI-split APKs are functionally
> identical to the universal — the splits are kept for distribution
> hygiene only. Pre-v0.29.x builds shipped MapLibre native libraries
> per architecture, which made the splits genuinely different sizes
> (~13–16 MB each).

All Android dependency versions are pinned in
[`android/gradle/libs.versions.toml`](android/gradle/libs.versions.toml).
Floating versions (`1.+`) are deliberately avoided — they turn every
build into an unauditable supply-chain risk.

## Screenshots

Layout overview:

```
┌──────────────────────────────────────────────┐
│  Transfer Rate              ⓘ  ⚙  ↻           │
├──────────────────────────────────────────────┤
│  ╔════════════════════════════════════════╗  │
│  ║  1 AED  🇮🇳                              ║  │  Mid-market header
│  ║  = 25.8384 ₹                           ║  │  (Google Finance)
│  ║  Indian Rupee · Mid-market rate        ║  │
│  ║  Updated 12 minutes ago · Google Finance║ │
│  ╚════════════════════════════════════════╝  │
│  ╭─────────────────────────────────────────╮ │
│  │ 💡 Welcome                            ✕ │ │  First-launch hint
│  │ The big number above is the mid-market…│ │  (dismissible)
│  ╰─────────────────────────────────────────╯ │
│  ┌─Sending─────────────────────────────────┐ │  Variable amount input
│  │ AED [3,000]                             │ │
│  └─────────────────────────────────────────┘ │
│  [1k] [5k] [10k] [25k] [50k]                 │  Quick-pick chips
│                                              │
│  ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓ │
│  ┃ [A] Aspora    BEST           25.8200  ┃ │  Best provider
│  ┃     within minutes       -0.0184 vs mid┃ │  (gold border)
│  ┃     You receive        ₹ 77,460.00    ┃ │
│  ┃     ~~~~~~~~~~~~~~                    ┃ │  7-day sparkline
│  ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛ │
│  ┌────────────────────────────────────────┐ │
│  │ [W] Wise                     25.8041  │ │
│  │     within minutes       -0.0343 vs   │ │
│  │     You receive        ₹ 77,412.30    │ │
│  └────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────┐ │
│  │ [R] Remitly                  25.7700  │ │
│  │     cash, bank, UPI      -0.0684 vs   │ │
│  │     You receive        ₹ 77,310.00    │ │
│  │  ┌──────────────────────────────┐     │ │
│  │  │ 25.9500 ₹ First ≥ AED 3500   │     │ │  Promo badge
│  │  └──────────────────────────────┘     │ │
│  └────────────────────────────────────────┘ │
│                                              │
│  Rates indicative. Not financial advice.     │
└──────────────────────────────────────────────┘
```

To capture real screenshots once installed:

```bash
# With phone connected via ADB
adb exec-out screencap -p > docs/screenshots/main.png
```

Recommended captures: home (light + dark), about screen, amount slider in
use, dark-mode delta indicators.

The data comes from a Python scraper that runs on GitHub Actions and writes a
single JSON file to GitHub Pages — no servers to operate, $0 ongoing cost.

```
                ┌──────────────────────────┐
   every 15 min │  GitHub Actions cron     │ scrapes
        ───────►│  scrapers/run_all.py     ├──────►  provider sites/APIs
                └──────────┬───────────────┘
                           │ commits JSON
                           ▼
                ┌──────────────────────────┐
                │  GitHub Pages (free CDN) │
                │  rates.json              │
                └──────────┬───────────────┘
                           │ HTTPS GET
                           ▼
                ┌──────────────────────────┐
                │  Android app (Compose)   │
                └──────────────────────────┘
```

## Providers

Active scrapers:

| Provider | Status | Source |
|----------|--------|--------|
| Wise | working | Public comparisons API |
| Aspora | working | Public rates API |
| Remitly | working | Public calculator page |
| TransferGo | working | Public FX API |
| Al Ansari Exchange | working | Public WordPress AJAX calculator |
| Al Dahab Exchange | working/stale-tolerant | Public homepage rate marquee |
| Ahalia Exchange | working | Public inline `cc_data` rates |
| Federal Exchange | working | Public homepage rate card |
| GCC Exchange | working | Public rates API |
| Index Exchange | working | Public homepage rate field |
| Orient Exchange | working | Public `/Orient/GetExchangeRates` JSON endpoint |
| Lari Exchange | working | Public server-rendered rate table |
| Sharaf Exchange | working | Public `/engine/wp-json/v1/currency-exchange-rates` JSON endpoint |

Not active in the live app until a reliable public source is confirmed:

| Provider | Current finding |
|----------|-----------------|
| LuLu Money | Dropped in v0.30.6 because F5 BIG-IP WAF blocks cloud datacenter IPs used by GitHub Actions and Cloudflare Workers. |
| e& Money | App-only / no stable public web rate endpoint confirmed. |
| Botim Pay | App-only / no stable public web rate endpoint confirmed. |
| Comera | App-only / no stable public web rate endpoint confirmed. |
| Careem Pay | Not in the active scraper registry; needs a re-check before listing. |
| Al Rostamani, Al Fardan, Joyalukkas, Al Ghurair, Wall Street | Dubai/UAE candidates under investigation; Al Fardan is blocked by a Sucuri CloudProxy WAF challenge. |


## Repository layout

```
transfer-rate/
├── CHANGELOG.md               Canonical version history (see also
│                              docs/CHANGELOG.html for the rendered report)
├── scrapers/                  Python scrapers + orchestrator
│   ├── base.py                Provider/Quote interface
│   ├── utils.py               HTTP client (polite User-Agent, timeouts)
│   ├── run_all.py             Runs everything, writes public/rates.json
│   ├── gold.py                UAE gold (Khaleej Times) + India gold/silver
│   │                          (LiveChennai) + UAE silver (XAG spot × peg)
│   ├── wise.py, remitly.py, transfergo.py, orient_exchange.py, aspora.py, …
├── public/
│   ├── rates.json             Output, served by GitHub Pages
│   └── admin/                 Static manual-entry admin UI
├── android/                   Android 14+ Compose app
│   ├── app/                   :app module (UI, data, theme, workers)
│   ├── settings.gradle.kts
│   ├── build.gradle.kts
│   ├── gradle/libs.versions.toml   Pinned dependency versions
│   ├── keystore.properties.example       Release-signing keystore template (gitignored .properties)
│   └── secrets.properties.example        Refresh-trigger bearer template (gitignored .properties)
├── infra/
│   ├── lulu-proxy/            Cloudflare Worker — bearer for LuLu's public rate API
│   └── lulu-residential/      Self-hosted fallback runner for LuLu (legacy)
├── fastlane/
│   └── metadata/android/en-US/    Play Store listing copy
├── .github/workflows/
│   ├── scrape.yml             Cron */15, runs scrapers, deploys Pages
│   ├── scrape-lulu-residential.yml   Self-hosted runner for LuLu fallback
│   ├── android-build.yml      Builds + signs APKs on tag push
│   ├── changelog.yml          Validates CHANGELOG sync on tag push
│   └── test.yml               Python scraper unit tests
└── docs/
    ├── ARCHITECTURE.md        Two-plane design, failure model, schema
    ├── RUNBOOK.md             Operator playbook (manual entries, incidents)
    ├── PUBLISHING.md          Play Store release procedures
    ├── USER_GUIDE.md          End-user-facing app documentation
    ├── CHANGELOG.html         Rendered version history (auto-generated)
    └── report.html            Hand-curated technical narrative
```

> **Refresh-trigger Worker is not in this repo.** The Cloudflare Worker
> that holds the GitHub PAT and dispatches the scrape workflow on
> Refresh-button taps (`transfer-rate-refresh.imranbatchait.workers.dev`)
> lives only in the Cloudflare dashboard — it's not checked in. Only
> the lighter `infra/lulu-proxy/` Worker (which holds public LuLu
> credentials) ships here.

## Documentation map

| Audience | Document |
|----------|----------|
| End user | [`docs/USER_GUIDE.md`](docs/USER_GUIDE.md) |
| Anyone wanting the version history | [`CHANGELOG.md`](CHANGELOG.md) |
| Anyone wanting a printable / shareable changelog | [`docs/CHANGELOG.html`](docs/CHANGELOG.html) |
| Maintainer / architecture | [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) |
| Maintainer / on-call | [`docs/RUNBOOK.md`](docs/RUNBOOK.md) |
| Releaser | [`docs/PUBLISHING.md`](docs/PUBLISHING.md) |
| Security / privacy / takedown | [`SECURITY.md`](SECURITY.md), [`PRIVACY.md`](PRIVACY.md), [`DISCLAIMER.md`](DISCLAIMER.md), [`TAKEDOWN.md`](TAKEDOWN.md) |

## Manual rate entry

For providers without a public rate endpoint (app-only services like Botim,
e&amp; Money, Comera, Careem Pay), the maintainer can enter rates manually
through a static admin page:

* **URL**: <https://imraneggy.github.io/transfer-rate/admin/>
* **Auth**: fine-grained PAT with `contents: write` on this repo, stored
  in browser localStorage
* **Effect**: rates appear in the app with a `MANUAL` badge after the
  next cron tick

See [`docs/RUNBOOK.md`](docs/RUNBOOK.md#manual-rate-entry-admin-ui) for the
full operating instructions.

## Quick start (developer)

### Run scrapers locally

```bash
python -m venv .venv
source .venv/bin/activate           # Windows: .venv\Scripts\activate
pip install -r requirements.txt
python -m scrapers.run_all --out public/rates.json
cat public/rates.json
```

### Build the Android app

Prerequisites: Android Studio Hedgehog or newer, JDK 17, Android SDK 35.

```bash
cd android
./gradlew :app:assembleDebug      # outputs app/build/outputs/apk/debug/
./gradlew :app:installDebug       # to a connected Android 14+ device
```

To enable the Refresh-button → upstream-scrape feature in a local debug
build, copy `android/secrets.properties.example` to
`android/secrets.properties` and paste the bearer secret you maintain
in the Cloudflare Worker (variable name `SHARED_SECRET` Cloudflare-side,
`REFRESH_TRIGGER_SECRET` build-side; values must match). Without the
file the feature degrades gracefully — the button becomes a silent
no-op, the 15-minute cron continues regardless.

### Deploy

Push to `main`. The `scrape` workflow runs on a 15-minute cron and on
every push that touches `scrapers/`; it commits the latest `rates.json`
to `public/` and deploys the directory to GitHub Pages in the same
workflow (no separate `pages.yml`). Pages source is configured once in
repo settings:

```
Settings → Pages → Source: GitHub Actions
```

Tag pushes (`v*.*.*`) trigger `android-build.yml`, which produces signed
release APKs and creates a GitHub Release. The companion
`changelog-sync` workflow fails the tag push if either `CHANGELOG.md` or
`docs/CHANGELOG.html` is missing an entry for the new version — see the
[release checklist](CONTRIBUTING.md#release-checklist-maintainers).

## Security & privacy

* **Permissions:** `INTERNET` and `ACCESS_NETWORK_STATE`. Optional
  `POST_NOTIFICATIONS` only if the user opts into daily-high alerts.
  No location permission since v0.29.0 (mosque finder removal).
* **Outbound hosts allowlisted at the OkHttp layer** (real enforcement,
  not just a `network_security_config.xml` policy hint): only
  `imraneggy.github.io` (rates JSON) and
  `transfer-rate-refresh.imranbatchait.workers.dev` (refresh dispatch)
  are reachable. See `data/NetworkSecurity.kt`.
* Cleartext HTTP forbidden by `network_security_config.xml` globally.
* Strict TLS via OkHttp defaults; no certificate pinning (CDN cert
  rotation makes pins brittle).
* `rates.json` schema-validated and bound-checked before render — no
  `NaN`/`inf`, no stratospheric rates, URL fields scheme-allowlisted to
  `https://` only (defends against `intent://`, `app://`, `file://` in
  a poisoned doc reaching `Intent.ACTION_VIEW`).
* The Cloudflare Worker bearer secret is no longer hardcoded in
  `build.gradle.kts` (since v0.28.1) — read from the
  `REFRESH_TRIGGER_SECRET` env var (CI) or `android/secrets.properties`
  (local), gitignored both ways.
* No analytics, no telemetry, no crash-reporting SDK, no
  Google Play Services dependency.
* No data is collected, stored, or transmitted off-device.

See [`SECURITY.md`](SECURITY.md), [`PRIVACY.md`](PRIVACY.md),
[`DISCLAIMER.md`](DISCLAIMER.md), and [`TAKEDOWN.md`](TAKEDOWN.md).

## Legal posture for scraping

We scrape **only public marketing/rate pages**. We do not authenticate, do not
replay private mobile APIs, do not cache PII (there is none on these pages),
and we identify our bot in the User-Agent string with a contact link.

Any provider can request removal at any time — see `TAKEDOWN.md`. We act
within 24 hours.

## Distribution

The app is distributed via:

* **Google Play** — release-signing config wired in
  `app/build.gradle.kts`; per-architecture APK splits enabled.
  Release procedure in [`docs/PUBLISHING.md`](docs/PUBLISHING.md).

For local release builds:

```bash
# 1. Generate a keystore (one-time) — see docs/PUBLISHING.md
# 2. Create android/keystore.properties from the .example file
cd android
./gradlew :app:assembleRelease
# Outputs to app/build/outputs/apk/release/
```

## License

Proprietary — © 2026 Transfer Rate, all rights reserved. See
[`LICENSE`](LICENSE). This source is published for transparency only; no
licence is granted to use, copy, modify, or redistribute it.
