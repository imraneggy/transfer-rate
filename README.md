# transfer-rate

Open-source AED → INR rate aggregator for UAE remittance services.
Public, free, ad-free, no analytics, no accounts.

[![scrape](https://github.com/imraneggy/transfer-rate/actions/workflows/scrape.yml/badge.svg)](https://github.com/imraneggy/transfer-rate/actions/workflows/scrape.yml)
[![pages](https://github.com/imraneggy/transfer-rate/actions/workflows/pages.yml/badge.svg)](https://github.com/imraneggy/transfer-rate/actions/workflows/pages.yml)
[![license](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

## What this is

A small Android 14+ app that shows current 1 AED → INR rates from major UAE
remittance services side-by-side. Updates every 15 minutes.

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
├── scrapers/                  Python scrapers + orchestrator
│   ├── base.py                Provider/Quote interface
│   ├── utils.py               HTTP client (polite User-Agent, timeouts)
│   ├── run_all.py             Runs everything, writes public/rates.json
│   ├── wise.py, remitly.py, …
├── public/
│   └── rates.json             Output, served by GitHub Pages
├── android/                   Android 14+ Compose app
│   ├── app/
│   ├── settings.gradle.kts
│   ├── build.gradle.kts
│   └── gradle/libs.versions.toml
├── .github/workflows/
│   ├── scrape.yml             Cron */15, runs scrapers, commits JSON
│   └── pages.yml              Publishes public/ to GitHub Pages
└── docs/
    ├── ARCHITECTURE.md
    ├── RUNBOOK.md
    ├── PUBLISHING.md
    └── report.html
```

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
