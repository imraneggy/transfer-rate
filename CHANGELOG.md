# Changelog

All notable changes to **Transfer Rate** are documented in this file.

The format roughly follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

The canonical source of truth is this Markdown file. A rendered HTML version
lives at [`docs/CHANGELOG.html`](docs/CHANGELOG.html) and is regenerated
automatically by `.github/workflows/changelog.yml` on every `v*.*.*` tag push.

> **Versioning note.** Entries below from `v0.13` and earlier predate Transfer
> Rate's rebrand from *Exchangia*; commit subjects from that period are
> preserved verbatim for historical accuracy.

---

## [0.27.1] — 2026-05-08

### Fixed
- **Popup heading legibility — both light and dark mode.** All in-sheet
  text labels were styled with `MaterialTheme.colorScheme.outline`, which
  is the M3 role for **divider strokes** (light = `#A6C1DE` at L=80, dark
  = `#4A6684` at L=50) — designed to be too faint to read as text.
  Section headings ("30-day stats", "Recent days", "24K trend") now use
  `onSurface` + Bold + `labelMedium`. Secondary labels (table column
  headers, sources footer, sparkline unit labels, placeholder text) use
  `onSurfaceVariant`. Dividers continue to use `outlineVariant` — that
  one is the right role.
- **Mid-market header card on home — same-hue muddy contrast.** The big
  "1 AED → INR" card on the home screen used `primaryContainer` (pale
  indigo, L=95) as background with `onPrimaryContainer` (deep indigo,
  L=20) text. APCA contrast technically passed, but both colours sat in
  the same hue family so the eye registered the pair as muddy — there
  was no hue cut, only a lightness cut. Background switched to
  `surfaceVariant` (near-neutral slate) and the body text to `onSurface`
  (deep navy, distinct hue). The "MID-MARKET" eyebrow stays in
  `primary` colour so the brand identity reads as a tinted accent
  rather than a tinted slab.
- **"View full N-day history" sub-sheet button.** Same hue-collision
  fix: switched from `primaryContainer.copy(alpha = 0.55f)` bg +
  `onPrimaryContainer` text to `surfaceVariant` bg + `primary`-coloured
  bold text. Reads cleanly as a "view more" link without the muddy
  pale-purple-on-deep-indigo feel.
- `StatPill` label colour likewise corrected from `outline` to
  `onSurfaceVariant` so High / Low / Avg tags are readable.

### Removed
- Dead `_LegacySnapshotCard` composable (~130 lines) from
  `GoldHistorySheet.kt`. Kept across v0.26 and v0.27 as a "fallback in
  case we revert"; two releases on, the design isn't reverting and the
  dead code carried the same bad-colour-role usage we just fixed in the
  live code.

---

## [0.27.0] — 2026-05-08

### Changed
- **Primary palette lifted one step on the OKLCh ramp** to address feedback
  that Stripe indigo at L=60 read too dark against the paper-light
  background. Light-mode primary is now `#8486FF` (L=70) instead of `#635BFF`
  (L=60); dark-mode primary is `#A3ACFF` (L=80) instead of `#8486FF` (L=70).
  The previous primary anchors slot one step down to become the new
  secondary, so the dual-tone identity is preserved.
- **Gold/silver bottom sheet body restructured to a single `LazyColumn`.**
  All sections (header, snapshot grid, sparklines, carat selector, stats,
  recent days, footer) are now `item { }` blocks. This eliminates the
  nested-scroll trap that prevented the inline 30-row history from
  scrolling cleanly.

### Added
- **Dedicated full-history sub-sheet.** The inline 30-row table is now a
  5-row recent-days preview followed by a `View full {N}-day history →`
  button that opens a second `ModalBottomSheet`. The sub-sheet's body is a
  single `LazyColumn` so the table scrolls without bound — sheet-on-sheet
  pattern, dismissing returns to the overview without closing the parent.
- **Project documentation system.** `CHANGELOG.md` (this file),
  `docs/CHANGELOG.html` (rendered report), `docs/USER_GUIDE.md` (end-user
  doc), and `.github/workflows/changelog.yml` (auto-regenerates the HTML
  changelog on every release tag).

---

## [0.26.0] — 2026-05-07

### Added
- **Stripe Atlas Premium design refresh.** Full palette regenerated from
  OKLCh tonal ramps anchored on Stripe brand values: primary indigo
  `#635BFF`, neutral navy `#0A2540`, paper background `#F6F9FC`. APCA
  contrast ratings tracked per role.
- **Metal-specific tonal ramps.** Gold and silver now have their own warm
  (saddle) and cool (steel) ramps in `Theme.kt`, exposed via
  `LocalMetalColors`. Replaces the previous trick of borrowing from
  secondary + neutral for metal tinting.
- **Two-column gold/silver header card** with side-by-side metal columns,
  each rendered with its own gradient background (warm gold | cool steel).
- **Per-metal snapshot cards** in the bottom sheet — gold card shows
  24K + 22K rates per region, silver card shows 1g + 1kg per region.
- **Bumped display typography** (`displayLarge` 57sp → 72sp,
  `displayMedium` 45sp → 56sp, `displaySmall` 36sp → 40sp) with negative
  tracking for "hero numeral" rendering on the home rate card.

---

## [0.25.0] — 2026-05-07

### Changed
- Renamed the metal label "Ag" → "Silver" in all UI surfaces to remove
  the chemistry shorthand non-experts find confusing.
- Standardised silver weights to **1 g + 1 kg** (gold remains 1 g + 8 g).
  Reflects the bullion-purchase reality: silver is bought by the
  kilogram, gold by the gram or tola.
- Bolder sparkline strokes (1.6 dp → 2.4 dp) for legibility on small
  phone screens.

---

## [0.24.0] — 2026-05-07

### Added
- Silver columns in the gold/silver bottom sheet alongside gold. UAE
  silver is spot-only (no daily history); India silver has 10-day
  history from LiveChennai.

---

## [0.23.0] — 2026-05-07

### Changed
- **Switched India gold source from BankBazaar (Mumbai) to LiveChennai
  (Chennai).** LiveChennai publishes 10 days of historical rates per
  page, which feeds the new history sheet directly without requiring
  daily snapshots.
- Added silver scraping on both sides of the corridor:
  - **UAE silver** via `gold-api.com` XAG spot × the AED-USD peg (3.6725).
  - **India silver** parsed from LiveChennai's silver table.

---

## [0.22.0] — 2026-05-07

### Added
- **Refresh button now triggers a fresh upstream scrape.** Tapping the
  refresh icon dispatches a `workflow_dispatch` to the scrape workflow
  via the Cloudflare Worker proxy, so the user gets latest values on
  demand instead of waiting for the next 15-minute cron tick.

### Infrastructure
- Cloudflare Worker (`transfer-rate-refresh.imranbatchait.workers.dev`)
  holds the GitHub PAT in encrypted env so the APK never ships a token.
  Worker accepts a shared bearer secret and returns
  `{ "dispatched": true, "etaSeconds": 35 }`.

---

## [0.21.0] — 2026-05-07

### Fixed
- **Defeated all CDN caching on rates fetch.** Added a `?_t=<currentMillis>`
  cache-bust query parameter to every `RatesRepository` request after
  observing 3+ minute staleness from GitHub Pages' `Cache-Control: max-age=600`
  combined with Fastly edge cache.

---

## [0.20.0] — 2026-05-07

### Removed
- "Today's Best" in-app banner from the home dashboard. Status-bar
  notification (added in v0.19) covers the same need without consuming
  screen real estate.

---

## [0.19.0] — 2026-05-06

### Added
- **Opt-in status-bar notifications for new daily highs.** WorkManager
  periodic worker compares the current best rate to today's running
  high; a notification fires when a new high is set. User opts in from
  the About screen; channel uses `IMPORTANCE_DEFAULT`.

---

## [0.18.0] — 2026-05-06

### Added
- "Today's Best" banner above the rates list, showing the
  current-best provider plus today's running peak. (Reverted in v0.20
  in favour of notifications-only.)

---

## [0.17.0] — 2026-05-06

### Changed
- **App rebranded from "Exchangia" to "Transfer Rate".** New logo,
  wordmark, splash, palette, package name remains `com.transferrate.app`.

---

## [0.16.x] — 2026-05-04

- **0.16.3** — New launcher icon (Stair-Step E variant).
- **0.16.2** — User location marker on the mosque map; per-pin distance
  badge.
- **0.16.1** — Mosque finder UX cleanup (filter chips, smoother camera).
- **0.16.0** — Smoother MapLibre camera, location accuracy improvements,
  Lari (Georgia) provider logo refresh.

---

## [0.15.0] — 2026-05-04

### Added
- **Mosque finder.** New screen with MapLibre + OSM raster tiles
  (no API key, $0-ops). 12th provider added.

---

## [0.14.0] — 2026-05-03

### Added
- 22K gold trend in addition to 24K.
- Mid-market rate history sparkline.
- Typography pass: tightened tracking, bumped headline weights.

---

## [0.13.x] — 2026-05-03

- **0.13.6** — Word labels in the toolbar; LuLu workaround attempt notes.
- **0.13.5** — Hide LuLu when the residential proxy is unavailable
  (Playwright path was a dead end).
- **0.13.4** — Icon padding fix; tagline added; data-source attribution
  removed from About.
- **0.13.3** — Equal-height + bolder mid-market & gold headers.
- **0.13.2** — Launcher icon: soft-grey background + clean hero
  foreground.
- **0.13.1** — LuLu working in CI via Playwright headless Chromium.
- **0.13.0** — Brand-asset refresh from Media(2) design.

---

## [0.12.2] — 2026-05-03

### Fixed
- Dropped `?attr/colorControlNormal` from the launcher icon (no
  AppCompat theme available in the icon's render target).

---

## [0.11.0] — 2026-05-02

### Changed
- Rebrand to **Exchangia** — typography, icon, and palette
  (subsequently rebranded again to Transfer Rate in v0.17).

---

## [0.10.x] — 2026-05-02

- **0.10.1** — Ahalia Exchange (11th provider).
- **0.10.0** — TransferGo provider; redesigned icon, splash, theme polish.

---

## [0.9.x] — 2026-05-02

- **0.9.0 (later)** — Provider history sheet; provider logos; 10-day
  retention.
- **0.9.0** — WorkManager prefetch worker; distribution-ready release
  builds (signing, ABI splits, ProGuard rules).

---

## [0.8.x] — 2026-05-02

- **0.8.1** — Rate history sparklines; improved logo graphics.
- **0.8.0** — Variable amount input; pull-to-refresh; on-disk cache;
  better error states.

---

## [0.7.0] — 2026-05-02

### Added
- Three-bar logo and branded splash.

### Removed
- Unverified provider stubs trimmed from the rates list.

---

## [0.6.0] — 2026-05-02

### Changed
- App scoped to the **AED → INR corridor only** (was multi-corridor).

---

## [0.5.x] — 2026-05-02

- **0.5.1** — Minimalist T-monogram icon and splash screen.
- **0.5.0** — Manual rate-entry admin UI (`/admin/`) for app-only
  providers (e&, Botim, Comera, Careem Pay).

---

## [0.4.0] — 2026-05-02

### Added
- Google Finance mid-market rate as the benchmark/header.
- Light/dark mode toggle.
- New launcher icon.
- Receive-amount estimates per provider.

---

## [0.3.0] — 2026-05-02

### Added
- App icon (initial design).
- Mid-market rate header.
- Ten provider stubs (most marked `investigating`).

---

## [0.1.0] — 2026-05-01

### Added
- Initial scaffold: Python scrapers, Android 14+ Compose app, GitHub
  Actions CI, project documentation.

---

[0.27.1]: https://github.com/imraneggy/transfer-rate/releases/tag/v0.27.1
[0.27.0]: https://github.com/imraneggy/transfer-rate/releases/tag/v0.27.0
[0.26.0]: https://github.com/imraneggy/transfer-rate/releases/tag/v0.26.0
[0.25.0]: https://github.com/imraneggy/transfer-rate/releases/tag/v0.25.0
[0.24.0]: https://github.com/imraneggy/transfer-rate/releases/tag/v0.24.0
[0.23.0]: https://github.com/imraneggy/transfer-rate/releases/tag/v0.23.0
[0.22.0]: https://github.com/imraneggy/transfer-rate/releases/tag/v0.22.0
[0.21.0]: https://github.com/imraneggy/transfer-rate/releases/tag/v0.21.0
[0.20.0]: https://github.com/imraneggy/transfer-rate/releases/tag/v0.20.0
[0.19.0]: https://github.com/imraneggy/transfer-rate/releases/tag/v0.19.0
[0.18.0]: https://github.com/imraneggy/transfer-rate/releases/tag/v0.18.0
[0.17.0]: https://github.com/imraneggy/transfer-rate/releases/tag/v0.17.0
[0.15.0]: https://github.com/imraneggy/transfer-rate/releases/tag/v0.15.0
[0.14.0]: https://github.com/imraneggy/transfer-rate/releases/tag/v0.14.0
[0.11.0]: https://github.com/imraneggy/transfer-rate/releases/tag/v0.11
[0.7.0]: https://github.com/imraneggy/transfer-rate/releases/tag/v0.7.0
[0.6.0]: https://github.com/imraneggy/transfer-rate/releases/tag/v0.6.0
[0.4.0]: https://github.com/imraneggy/transfer-rate/releases/tag/v0.4.0
[0.3.0]: https://github.com/imraneggy/transfer-rate/releases/tag/v0.3.0
