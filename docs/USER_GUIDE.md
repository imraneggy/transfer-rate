# Transfer Rate — User Guide

> A short tour of the app for first-time users. Last updated for **v0.30.4**.
> Screenshots welcome — drop them in `docs/screenshots/` via PR.

---

## What the app does

Transfer Rate shows you, in one place:

1. **The current 1 AED → INR rate** from twelve UAE money-transfer
   services, ranked from best to worst.
2. **The Google Finance mid-market rate** as the benchmark, so you can
   see how much each provider takes off the top.
3. **Live gold and silver rates** for the UAE (Dubai) and India
   (Chennai) — useful when you want to convert savings into bullion
   on either side of the corridor.

There are no accounts, no ads, no analytics, and no sign-in. The app
makes one HTTPS request to a static JSON file on GitHub Pages and
renders what it finds.

---

## The home screen, top to bottom

### 1. Mid-market header

The big number at the top is **1 AED in Indian Rupees at the
mid-market rate**, sourced from Google Finance. This is the rate
banks quote each other — no individual customer can actually transact
at this rate, but it's the honest benchmark every provider's spread
is measured against.

The label underneath shows the source and how long ago the rate was
last refreshed (e.g. *"Updated 12 minutes ago · Google Finance"*).

### 2. Gold & silver header

Next to (or below) the mid-market header on the home screen is a
two-column **Gold | Silver** card:

- **Gold column** (warm tint): UAE 24K and 22K rates per gram in AED,
  with the equivalent INR rate underneath.
- **Silver column** (cool tint): UAE silver per gram and per kilogram
  in AED, plus the INR equivalent.

Tap anywhere on the card to open the **gold/silver bottom sheet**
(see "Gold sheet", below).

### 3. Welcome card (first launch only)

A one-time hint explaining the mid-market rate vs. provider rate
distinction. Dismiss with the × button — it won't reappear.

### 4. Sending amount

The **AED [3,000]** input lets you change the source amount. Below
it are quick-pick chips for **1k / 5k / 10k / 25k / 50k**.

The receive amounts shown on each provider card update live as you
change this number.

### 5. Provider cards

Each provider gets a card with:

- **Logo + name** on the left.
- **Their rate** as the headline number (large, monospaced digits).
- **Speed** (e.g. *"within minutes"*, *"1–3 days"*).
- **Spread vs mid-market** as a signed number (e.g. `-0.0184 vs mid`).
- **Estimated INR you'd receive** for the entered amount, after the
  provider's fee.
- A **7-day sparkline** showing the rate trend.
- An optional **promo badge** when the provider has a first-transfer
  bonus that beats the mid-market rate.

The single best provider gets a **gold border + "BEST" tag** at the
top of the list.

### 6. Refresh button (toolbar, top right)

Tapping refresh **triggers a fresh upstream scrape**, not just a
re-fetch of the cached JSON. The app pings a Cloudflare Worker, which
dispatches the scrape workflow on GitHub Actions; new rates land in
about 30–45 seconds. The button shows a spinner while waiting.

### 7. Footer

> *Rates indicative. Not financial advice.*

This is a real disclosure, not boilerplate. The app shows what
providers publish on their public marketing pages — actual quoted
rates at transfer time may differ.

---

## Gold sheet (tap the gold/silver card)

The gold/silver bottom sheet drills into the details for both metals:

1. **Snapshot grid.** Today's rates per region per carat (for gold)
   and per weight (for silver), in both AED and INR.
2. **Trend sparklines.** 24K + 22K gold trends, plus a silver trend
   (India only — UAE silver is spot-only with no daily history).
3. **Carat selector chips.** Tap **24K**, **22K**, or **Ag** to
   choose what the stats and history below refer to.
4. **30-day stats.** High / Low / Average for the selected
   carat/metal, per region.
5. **Recent days.** A 5-row preview of the most recent daily rates.
6. **View full {N}-day history.** Tapping this button opens a
   dedicated full-history popup (introduced in v0.27.0) with all
   available days as a scrollable table.

Sources are shown at the bottom of the sheet for transparency.

---

## Settings & About

### Notifications (opt-in)

The About screen has a toggle for **status-bar notifications when a
new daily-high rate is set**. If you turn it on:

- A WorkManager periodic task runs in the background.
- When today's best rate is exceeded, you get one notification at
  `IMPORTANCE_DEFAULT` priority.
- Tapping the notification opens the app on the home screen.

The notification permission is requested only when you toggle this
feature on.

### Theme

Light / dark mode follows the system setting by default. Material You
dynamic theming is **disabled** by default — Transfer Rate's crafted
palette is the consistent visual identity. Power users can enable
dynamic colour by editing `dynamicColor = true` in `Theme.kt` and
rebuilding.

### About

Lists the app version, source code link, license, and the
**Disclaimer / Privacy / Takedown** documents shipped with the APK.

---

## Data freshness

| Source | Refresh cadence |
|--------|-----------------|
| Provider rates (Wise, Remitly, etc.) | Every 15 minutes via cron |
| Mid-market (Google Finance) | Every 15 minutes via cron |
| Gold (UAE — Khaleej Times) | Every 15 minutes via cron |
| Gold (India — LiveChennai) | Every 15 minutes via cron |
| Silver (UAE — gold-api.com XAG × AED peg) | Every 15 minutes via cron |
| Silver (India — LiveChennai) | Every 15 minutes via cron |
| **On-demand (refresh button)** | ~30–45 seconds |

The 15-minute cron runs on GitHub's free tier and may be deprioritised
during peak hours — the **refresh button** is the fast path when
something matters. Each refresh-button tap dispatches a fresh
upstream scrape via the Cloudflare Worker.

---

## Privacy

- The app requests only the **`INTERNET`** permission, plus
  **`POST_NOTIFICATIONS`** *only if* you opt into daily-high alerts.
- No account, no sign-in, no analytics, no telemetry, no crash
  reporter SDK.
- Cleartext HTTP is forbidden by `network_security_config.xml`.
- Connections are allowlisted to `imraneggy.github.io` and
  `transfer-rate-refresh.imranbatchait.workers.dev`. Anything else is
  blocked at the network layer.
- Rates JSON is bound-checked before display (no negative rates, no
  NaN/inf, no implausibly high values from poisoned input).

See [`PRIVACY.md`](../PRIVACY.md) and [`SECURITY.md`](../SECURITY.md)
for the full posture.

---

## FAQ

**Why does the rate I see in the app differ slightly from the
provider's app?** The app shows the latest rate published on the
provider's marketing page, scraped on a 15-minute cycle. The
provider's own app/website may quote a transaction-time rate that
differs by a few basis points. Tap **refresh** to pull the latest
upstream snapshot.

**The "BEST" provider keeps changing — is the data unstable?** No —
that's the corridor. UAE→INR providers compete tightly and the lead
changes hands several times a day. The 7-day sparkline on each card
shows the longer-term trend.

**Can I add a provider you don't track?** Yes — adding a scraper is
a 30-minute job following `scrapers/wise.py` as a template. See
[`CONTRIBUTING.md`](../CONTRIBUTING.md).

**Why is silver per-kg shown for India but per-gram for both?** Silver
bullion is bought by the kilogram in practice; gold by the gram or
tola. The sheet shows both weights so the rates ladder match how
people actually buy each metal.

**Why is UAE silver "spot only — no daily history"?** Khaleej Times
doesn't publish a daily silver page comparable to its gold page. The
UAE silver number is computed from the live spot XAG price (USD per
ounce, from gold-api.com) multiplied by the AED-USD peg (3.6725) and
converted per gram. It's accurate to the moment but has no history.

**Will you publish on the Play Store / F-Droid?** Yes — both channels
are configured. See [`docs/PUBLISHING.md`](PUBLISHING.md).

---

## Need help?

- File an issue at <https://github.com/imraneggy/transfer-rate/issues>.
- Read the [`docs/RUNBOOK.md`](RUNBOOK.md) if you maintain the
  scrapers and need to triage a stuck rate.
- For provider takedown requests, see [`TAKEDOWN.md`](../TAKEDOWN.md) —
  acted on within 24 hours.
