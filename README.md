# Transfer Rate

Open-source AED → INR rate aggregator for UAE remittance services, with
side-by-side gold and silver rates for the UAE / India corridor.
Public, free, ad-free, no analytics, no accounts.

[![scrape](https://github.com/imraneggy/transfer-rate/actions/workflows/scrape.yml/badge.svg)](https://github.com/imraneggy/transfer-rate/actions/workflows/scrape.yml)
[![android-build](https://github.com/imraneggy/transfer-rate/actions/workflows/android-build.yml/badge.svg)](https://github.com/imraneggy/transfer-rate/actions/workflows/android-build.yml)
[![license](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

> **Latest release:** see [`CHANGELOG.md`](CHANGELOG.md) for the full
> version history; the rendered HTML report lives at
> [`docs/CHANGELOG.html`](docs/CHANGELOG.html). End-user docs are in
> [`docs/USER_GUIDE.md`](docs/USER_GUIDE.md).

## What this is

A small Android 14+ app that shows three things side-by-side, refreshed
every 15 minutes:

1. **AED → INR remittance rates** from twelve UAE money-transfer
   providers (Wise, Remitly, LuLu, Aspora, Careem Pay, TransferGo,
   Ahalia, and others).
2. **The Google Finance mid-market rate** as a benchmark, so the
   provider spread is visible at a glance.
3. **Gold & silver rates** for the UAE (Khaleej Times) and India
   (LiveChennai), with 24K + 22K gold (1 g + 8 g) and silver (1 g +
   1 kg), plus 30-day history.

A built-in **mosque finder** uses MapLibre + OpenStreetMap tiles
(no API key, $0-ops) to surface nearby mosques while travelling.

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
| Android — maps | MapLibre Native Android SDK | **11.11.0** |
| Android — maps | MapLibre Annotation Plugin v9 | **3.0.2** |
| Android — fonts | Manrope, Space Grotesk (OFL 1.1) | bundled |
| Data pipeline | Python (scrapers + orchestrator) | **3.11+** |
| Data pipeline | GitHub Actions (cron `*/15`) | hosted |
| Data hosting | GitHub Pages (Fastly CDN) | static |
| Refresh proxy | Cloudflare Worker (free tier) | hosted |
| Distribution | Google Play (release-signed AAB) | per-ABI splits |
| Distribution | F-Droid (reproducible from source) | metadata in `fastlane/` |

All Android dependency versions are pinned in
[`android/gradle/libs.versions.toml`](android/gradle/libs.versions.toml).
Floating versions (`1.+`) are deliberately avoided — they turn every
build into an unauditable supply-chain risk.

## Screenshots

Real screenshots welcome — submit a PR with PNGs in `docs/screenshots/`.
For now, here is the layout:

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

| Provider     | Status        | Source                 |
|--------------|---------------|------------------------|
| Wise         | working       | Public comparisons API |
| Remitly      | working       | Public calculator API  |
| LuLu Money   | working       | Public rate page       |
| Aspora       | working       | Public landing page    |
| Careem Pay   | working       | Public rate page       |
| e& Money     | investigating | App-only               |
| Botim Pay    | investigating | App-only               |
| Comera       | investigating | App-only               |

`investigating` providers appear in the app with a "Coming soon" badge.
Contributions to add scrapers for them are welcome — see `CONTRIBUTING.md`.

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
│   ├── wise.py, remitly.py, lulu.py, aspora.py, careem.py, …
├── public/
│   ├── rates.json             Output, served by GitHub Pages
│   └── admin/                 Static manual-entry admin UI
├── android/                   Android 14+ Compose app
│   ├── app/                   :app module (UI, data, theme, workers)
│   ├── settings.gradle.kts
│   ├── build.gradle.kts
│   └── gradle/libs.versions.toml   Pinned dependency versions
├── infra/
│   ├── lulu-proxy/            Residential-proxy helper for LuLu
│   ├── lulu-residential/      Playwright-based fallback (legacy)
│   └── refresh-worker/        Cloudflare Worker source (PAT custodian)
├── .github/workflows/
│   ├── scrape.yml             Cron */15, runs scrapers, deploys Pages
│   ├── android-build.yml      Builds + signs APKs on tag push
│   ├── changelog.yml          Regenerates docs/CHANGELOG.html on tag push
│   └── test.yml               Python scraper unit tests
└── docs/
    ├── ARCHITECTURE.md        Two-plane design, failure model, schema
    ├── RUNBOOK.md             Operator playbook (manual entries, incidents)
    ├── PUBLISHING.md          Play Store + F-Droid release procedures
    ├── USER_GUIDE.md          End-user-facing app documentation
    ├── CHANGELOG.html         Rendered version history (auto-generated)
    └── report.html            Hand-curated technical narrative
```

## Documentation map

| Audience | Document |
|----------|----------|
| End user | [`docs/USER_GUIDE.md`](docs/USER_GUIDE.md) |
| Anyone wanting the version history | [`CHANGELOG.md`](CHANGELOG.md) |
| Anyone wanting a printable / shareable changelog | [`docs/CHANGELOG.html`](docs/CHANGELOG.html) |
| New contributor | [`CONTRIBUTING.md`](CONTRIBUTING.md) + [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) |
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
cd transfer-rate
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

### Deploy

Push to `main`. The `scrape` workflow runs on schedule and after every push
to `scrapers/`. The `pages` workflow republishes whenever `public/` changes.
Set up GitHub Pages once in the repo settings:

```
Settings → Pages → Source: GitHub Actions
```

## Security & privacy

* App requests only `INTERNET` permission.
* Cleartext HTTP is forbidden by `network_security_config.xml`.
* Connections allowed only to `imraneggy.github.io` (domain allowlist).
* Strict TLS via OkHttp defaults; no certificate pinning (CDN cert rotation).
* JSON is bound-checked before being shown (no NaN/inf, no stratospheric
  rates from poisoned input).
* No analytics, no telemetry, no crash reporting SDK.
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

The app is being prepared for two distribution channels:

* **F-Droid** — metadata is in `fastlane/metadata/android/en-US/`.
  F-Droid builds from source; submission instructions in
  [`docs/PUBLISHING.md`](docs/PUBLISHING.md).
* **Google Play** — release-signing config wired in
  `app/build.gradle.kts`; per-architecture APK splits enabled.

For local release builds:

```bash
# 1. Generate a keystore (one-time) — see docs/PUBLISHING.md
# 2. Create android/keystore.properties from the .example file
cd android
./gradlew :app:assembleRelease
# Outputs to app/build/outputs/apk/release/
```

## License

MIT — see [`LICENSE`](LICENSE).

## Contributing

Read [`CONTRIBUTING.md`](CONTRIBUTING.md). Adding a new provider scraper is
a 30-minute job following `scrapers/wise.py` as a template.
