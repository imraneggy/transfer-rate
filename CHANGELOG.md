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

## [0.28.2] — 2026-05-08

### Changed
- **Refresh-button feels instant.** Reworked `RatesViewModel.performRefresh()`
  into a three-phase flow that surfaces the latest cron-published doc
  within ~1 second of tap, instead of holding the user behind a spinner
  for 30–50 seconds:
  - **Phase 0 (new):** immediate `rates.json` fetch — when the cron tick
    has produced a fresher doc than what's on screen, the new values
    land instantly. The spinner stays on (more updates may follow).
  - **Phase 1:** unchanged — POST the Cloudflare Worker to dispatch a
    fresh upstream scrape via `workflow_dispatch`.
  - **Phase 2 (tightened):** initial head-start cut from 6 s → 2 s; poll
    interval cut from 4 s → 2 s; max attempts raised 12 → 24 to keep
    the same ~48 s total budget. A truly-fresh upstream scrape now
    surfaces 0–4 seconds sooner than in v0.28.1.

  Total time-to-truly-fresh data is unchanged (still bound by GitHub
  Actions queue + scraper runtime + Pages CDN propagation, which we
  can't make faster from the client). What changed is the **perceived**
  responsiveness — the user sees something update within a second
  instead of a half-minute spinner.

  When the upstream trigger fails (no Worker, network blip), Phase 0's
  fetch is now the final state and the spinner clears — the refresh
  button never feels worse than before, only faster.

---

## [0.28.1] — 2026-05-08

### Security
- **Cloudflare Worker bearer secret rotated, and the new value is no
  longer hardcoded in `build.gradle.kts`.** Resolution order at build
  time is now:
  1. Environment variable `REFRESH_TRIGGER_SECRET` — used by CI; held
     in GitHub Actions repo secrets and injected into the gradle build
     by `.github/workflows/android-build.yml`.
  2. `android/secrets.properties` (gitignored) — used for local debug
     builds. Template at `android/secrets.properties.example`.
  3. Empty string — F-Droid reproducible builds and contributors who
     don't need the refresh-trigger feature. The existing
     `takeIf { it.isNotBlank() }` guard at `RatesRepository.kt:38`
     already handles this case by disabling the refresh-button-to-Worker
     path entirely.

  The Cloudflare-side variable is named `SHARED_SECRET`; the
  GitHub/build-time variable is named `REFRESH_TRIGGER_SECRET`. The two
  names differ deliberately so a search across either side cannot
  collide; the values must match.

  Old secret `tr-refresh-J9k4Lm7Qw…Ie3` (still publicly visible in the
  v0.28.0 build.gradle.kts and earlier git history) is now dead — the
  Worker rejects it with `HTTP 401 Unauthorized`. Verified before ship.

### Notes for contributors
- `android/secrets.properties.example` is checked in as a template.
  Copy to `android/secrets.properties` and fill in the value to enable
  refresh-button testing in local debug builds.
- F-Droid + reproducible builds: leave the env var unset and don't
  create a `secrets.properties`. The build will pass; the refresh
  button becomes a silent no-op (the 15-min cron continues to update
  rates regardless).

---

## [0.28.0] — 2026-05-08

### Security
- **Real outbound host allowlist enforced at the OkHttp layer.** Android's
  `<network-security-config>` does not actually restrict which hosts the
  app can reach — `<domain-config>` only overrides cleartext / cert-anchor
  policy per host. The "allowlist" the prior comment claimed was therefore
  not enforced. Added `data/NetworkSecurity.kt` with a shared
  `HostAllowlistInterceptor` used by both `RatesRepository` and
  `OverpassService`; an outbound HTTPS request to anything outside
  `{imraneggy.github.io, transfer-rate-refresh.imranbatchait.workers.dev,
  overpass-api.de, *.tile.openstreetmap.org}` now throws. The
  `network_security_config.xml` block was rewritten to (a) accurately
  describe what the platform layer does and does not do, and (b) cover
  `overpass-api.de` and `tile.openstreetmap.org` so per-host cleartext +
  cert-anchor policy matches the application-level allowlist.
- **TLS verification re-enabled on `scrapers/lari.py`.** Was
  `verify=False`; a MITM at the GHA-runner→Lari path could have poisoned
  `rates.json` with an arbitrary "Lari rate" that would be committed to
  `main` and shown to all users. Now verifies against `certifi.where()`
  by default, with an opt-in path for shipping `scrapers/certs/lari-chain.pem`
  if the chain genuinely fails default trust. Added a corridor-aware
  bound check (AED→INR rate must fall in 20..32) so a parsed-but-poisoned
  numeric rate is still rejected.
- **Disabled redirect following on both OkHttp clients.** A 3xx from an
  allowed host pointing to an attacker-controlled host would otherwise
  be chased silently with attacker headers in tow.
- **Admin UI Content Security Policy added.** `public/admin/index.html`
  now ships `default-src 'none'; connect-src https://api.github.com;
  frame-ancestors 'none'; base-uri 'none'` plus COOP/COEP, all delivered
  via `<meta http-equiv>` since GitHub Pages can't set custom headers.
  Closes the obvious XSS-to-PAT-exfiltration path.
- **Admin UI PAT now expires after 8 hours.** Storage envelope changed
  from raw `localStorage` to `{pat, savedAt}` with a TTL check on read.
  v1 keys are auto-migrated on first load. The "Forget token" button
  clears both v1 and v2 keys.
- **Admin UI quick-paste commit message no longer leaks the rate value.**
  The provider name stays (it's already in the file diff); the rate goes
  away (was redundant with the diff and exposed precise entry-time
  numerics in the public commit history).
- **Provider URLs from `rates.json` are now scheme-validated** in
  `Rates.kt::validate()`. Only `https://` survives — defends against a
  poisoned doc using `intent://...`, `app://...`, `file:///...`, or
  `content://...` to launch arbitrary registered activities via
  `Intent.ACTION_VIEW`.
- **String fields in `rates.json` are now length-bounded** (providerId
  / providerName ≤ 64, note / promoNote ≤ 256, deliveryEstimate ≤ 64,
  url ≤ 256). Belt-and-braces against a poisoned doc shipping a 1 MB
  providerName that would blow notification body strings or layout
  heuristics.
- **CI release builds now error if no signing keystore is configured.**
  Was a silent fallback to the (shared) debug keystore — risk of a
  release tag accidentally producing a debug-signed APK that other
  attackers can over-install. Local builds still permit the fallback
  with a loud warning so first-time contributors and reproducible-build
  verifiers can run `assembleRelease` without a keystore at hand.
- **`android-build.yml` permissions are now job-scoped.** Was top-level
  `contents: write`; now `permissions: {}` at workflow level with
  `contents: write` granted only on the `build` job (which is the only
  one that needs it for `softprops/action-gh-release`).
- **`changelog.yml` now SHA-pins `actions/checkout`** (was the mutable
  `@v4` tag) and runs on `ubuntu-24.04` (was `ubuntu-latest`), matching
  the SHA-pinning + Ubuntu version used by every other workflow.
- **Validate `_doc` field type in admin UI before round-tripping.**
  Previously passed verbatim from `currentDoc._doc` to the new commit;
  now type-checked as a string and dropped if not.
- **`infra/lulu-proxy/worker.js` token cache TTL reduced** 5 min → 2
  min so a rotated upstream credential propagates faster after redeploy.
- **`infra/lulu-proxy/README.md` security stance corrected** — the prior
  text claimed payload-signature filtering that the worker never
  implemented (it ignores the request body and issues a fixed upstream
  call, which is *more* restrictive). Documentation now matches code.
- **`docs/RUNBOOK.md` no longer teaches the `cat git.txt` PAT pattern.**
  Replaced with `${GH_TOKEN:?...}` env-var pattern + a `gh workflow run`
  alternative that uses the OS keychain.
- **Play Store description** (`fastlane/.../full_description.txt`) now
  describes the actual allowlist semantics (application-layer) and
  acknowledges the optional location + notification permissions —
  previously claimed "single permission: INTERNET" which became
  inaccurate when the mosque finder shipped.

### Changed
- **Capped in-app font scale at 1.15×.** On phones with a system font
  scale of 1.3× or above, "Aspora" was wrapping to "Asp\nora" or
  ellipsizing to "Asp..." in the rates list because the provider-name
  column is contested with the BEST/MANUAL badge and the rate column.
  Cap installed via a `CompositionLocalProvider` of `LocalDensity` at
  the AppRoot. Users wanting larger text beyond 1.15× can use
  system-level magnification (which scales the whole UI uniformly).
- **Reduced provider-name fontSize 16sp → 15sp** for additional
  breathing room next to the BEST/MANUAL badge even at the 1.15× cap.
- **Mid-market history popup now shows the app logo** instead of a
  generic "MR" colored chip. The `ProviderAvatar` widget special-cases
  `providerId == "mid_market"` and renders `R.drawable.ic_splash` on
  the same white-circle treatment as a real provider logo.
- **Top app bar now shows the app logo to the left of "Transfer Rate"**
  (24 dp icon + 8 dp gap). The maxLines/ellipsize defence on the
  wordmark stays intact.

### Removed
- Dead `_LegacySnapshotCard` composable (~130 lines) from
  `GoldHistorySheet.kt`.

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

[0.28.2]: https://github.com/imraneggy/transfer-rate/releases/tag/v0.28.2
[0.28.1]: https://github.com/imraneggy/transfer-rate/releases/tag/v0.28.1
[0.28.0]: https://github.com/imraneggy/transfer-rate/releases/tag/v0.28.0
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
