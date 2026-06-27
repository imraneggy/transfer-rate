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

## [0.39.1] - 2026-06-27

### Fixed
- **Theme toggle icon.** The top-bar theme button showed a settings gear in System mode, making it look like a settings button rather than a light/dark toggle. It now shows a sun or moon reflecting the theme currently in effect (System resolves to the live system theme), so the button reads as a theme toggle at a glance.

## [0.39.0] - 2026-06-27

### Added
- **UAE jewellers directory.** The gold sheet now lists major UAE gold jewellers (Joy Alukkas, Malabar Gold & Diamonds, Kalyan Jewellers, Sky Jewellery, Damas), each a tap-to-open link to that jeweller's official daily-rate page. The UAE retail gold rate is set uniformly by the Dubai Gold & Jewellery Group, so this is a directory — not a price comparison; making charges (the real per-shop difference) vary by store and are noted as such rather than fabricated.

## [0.38.6] - 2026-06-26

### Fixed
- **Navy bars behind the splash card.** On real devices a thin navy tint showed at the top and bottom of the post-intro brand card, because the Fit-scaled card left letterbox bars through which the intro animation's navy final frame was visible. A full-screen black layer now sits behind the card, so the splash is pure black edge-to-edge on every screen aspect.

## [0.38.5] - 2026-06-26

### Changed
- **Full-black splash background.** The brand flash card ("Proud of UAE") shown after the intro animation now sits on a pure-black background instead of deep navy, for a cleaner OLED-true-black reveal. The card artwork and the splash container background were both updated.

## [0.38.4] - 2026-06-21

### Changed
- **Skeleton loading screen.** The rates list now shows an animated shimmer skeleton that mirrors the real layout (currency chips, hero rate + gold module, amount field, and provider rows) while data loads, instead of a centred spinner. The screen keeps its shape so content no longer visibly reflows when rates arrive.

## [0.38.3] - 2026-06-21

### Changed
- **UI polish pass.** Chip selections (currency, amount, karat) now animate their colour change instead of snapping; all chips meet the 48 dp minimum touch target; brand and semantic colours are centralized as theme tokens; the rate column holds a stable width to prevent jitter on refresh; promo badges animate in and out.
- **Back buttons.** The About and Upgrade screens now use a proper vector back arrow with a tint and accessibility label, replacing a plain "←" character.

### Fixed
- **Low-contrast secondary text.** Secondary text on the Upgrade screen used translucent overlays that failed contrast on the dark theme; it now uses the semantic on-surface-variant colour.
- **Small receive amounts.** Receive amounts under 100 (USD/EUR/GBP) now show two decimals instead of rounding down to "$ 0".

## [0.38.2] - 2026-06-21

### Fixed
- **Gold/silver popup stuck in INR.** Opening the gold/silver history sheet from any non-INR corridor now converts all India-side values into the selected currency, instead of always displaying INR.

## [0.38.1] - 2026-06-20

### Added
- **Brand flash card.** The splash sequence now shows a brief "infinity DXR" brand flash card after the reveal animation before settling on the rates list.

## [0.38.0] - 2026-06-16

### Added
- **Transfer Rate Pro.** Optional subscription ($0.99/month via Google Play Billing) unlocks up to 3 simultaneous rate-target alerts (free: 1), priority daily-high notifications, and supports independent development. The core app remains completely ad-free.
- **User personalization.** Per-device profile (display name, preferred sending amount, preferred corridor, favourite providers) stored locally in SharedPreferences — no account, no cloud sync.
- **Hosted privacy policy.** Public HTML privacy policy page at `public/privacy.html` (served via GitHub Pages), meeting Google Play's data safety requirement for a linked privacy policy URL.

### Changed
- **Toolbar icons.** The "AUTO / LIGHT / DARK" theme chip and "REFRESH" text chip in the top bar are now icon-only buttons (⚙ / ☀ / 🌙 and ↺ icons), freeing space so "Transfer Rate" no longer truncates to "Transfer…" on any screen width.
- **targetSdk bumped to 35.** Required by Google Play for all new submissions from August 2026.
- **PRIVACY.md updated.** Now discloses `POST_NOTIFICATIONS`, the Cloudflare refresh-trigger call, Google Play Billing data handling, and local SharedPreferences personalization; adds developer contact email.

### Fixed
- **Toolbar title truncation.** "Transfer Rate" was ellipsized to "Transfer…" on narrow phones (360 dp) because two labeled chips consumed most of the action row. Converting those chips to icon buttons permanently resolves the truncation.

## [0.37.0] - 2026-06-15

### Fixed
- **Invisible mid-market logo.** The Transfer Rate brand mark shown for the mid-market benchmark was a light glyph rendered on a near-white circle (the generic provider-avatar treatment) — effectively invisible. It now gets the same Deep Navy badge as the toolbar logo.
- **Gold/silver card stuck in INR.** The gold/silver rate card's India-side values now convert into the selected currency (via the AED mid-market rates) when you switch the currency chip, instead of always showing INR.

## [0.36.0] - 2026-06-15

### Added
- **8 more currency corridors.** AED -> PHP, BDT, EGP, USD, EUR, GBP, NPR, and LKR join the existing INR and PKR corridors (10 total), with feasibility live-verified across all scrapers. Use the currency chips to switch.
- **Animated splash screen.** The in-app splash now plays a full-bleed "infinity DXR" brand-reveal animation (the teal infinity money-flow loop drawing in, then the "Transfer Rate" wordmark and tagline) before settling on the rates list.

### Changed
- **Hide unavailable providers per currency.** When you switch currency, providers with no rate for that corridor (instead of showing an empty status dot) are hidden entirely from the list.
- **"infinity DXR" brand refresh.** New navy/teal logo, launcher icon, and splash colors across the app; tagline shortened to "Compare. Choose. Save."

### Fixed
- **Intermittent stale Lari rate.** lariexchange.com's ~2MB homepage routinely took 15-18s to download, occasionally exceeding the scraper's timeout and leaving the rate "stale" for hours. Raised Lari's client timeout to 30s and the orchestrator's per-call ceiling to 35s.

## [0.35.0] - 2026-06-14

### Added
- **HD provider avatars.** Added a real `logo_sharaf.png` (was missing entirely, falling back to initials) and replaced the blurry low-res `logo_orient_exchange.png` with a sharp 192x192 asset sourced from each provider's official site.
- **Motivational gold/silver calculator headlines.** The "what this buys" eyebrow above the gold/silver calculator now rotates through 30+ catchy, metal-specific phrases each time the screen opens or the metal selection changes: a purity-focused pool for 24K (incl. comparisons to 22K), a general gold-buying pool for 22K, and a silver-buying pool for Silver.

### Changed
- **Avatar fill.** Bundled provider logos now fill more of the avatar circle (padding reduced from 14% to 6%) so HD logos read clearly at small sizes.
- **Brand tints refreshed.** Updated the Orient Exchange BEST-card tint to match its real brand color, and added a new Sharaf Exchange tint.

### Removed
- **Provider-card sparklines.** Removed the 7-day rate trend sparkline rendered below each provider card to save vertical space; the ▲/▼ trend arrow next to the rate is unchanged.

## [0.34.0] - 2026-06-13

### Added
- **Sharaf Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.sharafexchange.ae/engine/wp-json/v1/currency-exchange-rates`, with fixture-backed parser tests.
- **Gold/silver "what this buys" calculator.** The amount section is now split into two cards: the existing Sending-amount input/chips, and a new calculator that converts the entered AED amount into grams of 24K gold, 22K gold, or silver at today's UAE rate, with a chip selector between the three.

## [0.33.0] - 2026-06-13

### Changed
- **Android visual facelift.** Hero mid-market rate card, gold/silver card, and provider cards now use a slightly larger corner radius (16-20dp -> 20-22dp) for a softer, more premium feel.
- **Depth treatment per theme.** In light mode, the hero, gold/silver, and provider cards now carry a subtle primary-tinted shadow; in OLED dark mode they instead get a faint light-catch gradient border, so cards read as raised surfaces against the pure-black background.
- **Hero rate glow.** The mid-market rate card now has a soft radial primary-color glow behind the headline rate, tuned lower-alpha for light mode and higher for dark mode.

## [0.32.7] - 2026-06-13

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.
- **Orient Exchange Android avatar.** Bundled `logo_orient_exchange.png` and added Orient brand tint mapping for BEST-card styling.

### Changed
- **Sending amount now updates live while typing.** Removed the `Set` button from the Sending amount field; entered amounts immediately update provider receive totals.
- **Quick amount chips updated.** The amount shortcuts now show AED 500, 1,000, 4,000, 6,000, and 10,000.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.

## [0.32.6] - 2026-06-13

### Added
- **Android stale-rate warning banner.** The home screen now shows a delayed warning when the loaded feed is 3-24 hours old and a critical stale warning after 24 hours, with the refresh action wired to the existing `RatesViewModel.refresh()` flow.
- **Debug APK validation workflow.** Added `android-test-build.yml` to compile `:app:assembleDebug` and upload the `transfer-rate-debug-apk` artifact before official release tagging.

### Fixed
- **CI debug builds no longer require release-signing secrets.** The Gradle release-signing guard now fails only when a release or bundle task is requested, so `:app:assembleDebug` can run safely in CI without keystore secrets.
- **Scrape workflow now commits generated rate artifacts again.** Replaced the invalid `git status --porcelain --cached` check with `git diff --cached --quiet`, restoring the intended git-backed history for `public/rates.json`, `public/history.json`, and `data/uae_gold_history.json`.
- **Gold scraper tests now match current sources.** UAE gold tests cover the igold.ae primary path and India gold tests cover LiveChennai history parsing.

### Internal
- Updated frontend and project analysis reports with the debug APK result, scrape verification, and release-readiness handoff.

## [0.32.5] — 2026-05-15

### Fixed
- **UAE gold history was being overwritten downstream of v0.32.4.**
  The scraper successfully produced 30 history points (confirmed in
  smoke tests) but `scrapers/run_all.py::_merge_uae_gold_history()`
  then blindly replaced `side["history"]` with whatever the rolling
  cron-snapshot file held — which was just 2 entries from before
  the igold swap.  Result: published `rates.json` had 30-day silver
  history but only 2-point gold history, even though both scrapers
  were working correctly.
  - Fixed by seeding the rolling dict with scraper-provided history
    FIRST, then layering the file-based rolling entries on top
    (using `setdefault` so the fresher scraper values win), then
    appending today's reading, then deduping + pruning.
  - End result: igold's 30 days seed the rolling file; the union
    is what the app sees.  Smoke tested:
    `Scraper: 30 points → After merge: 30 points`.

### Internal
- The `_merge_uae_gold_history` function was originally designed
  for the v0.x KT-only era when the scraper had no history of its
  own.  v0.32.4 made it obsolete by giving the scraper a real
  history source, but the merge step wasn't updated to match —
  classic "downstream pipeline didn't get the memo" bug.  The
  pattern (scrape → merge with stored state) is still valuable
  for graceful degradation across igold outages; just needs to
  respect scraper-provided history rather than override it.

## [0.32.4] — 2026-05-15

### Changed
- **UAE gold now uses igold.ae as primary source** (same provider
  that gives us UAE silver since v0.32.2).  Khaleej Times is the
  fallback when igold is unreachable.

  Why this matters: KT publishes a single-snapshot HTML page,
  not a daily history.  Our cron-snapshotted "history" was
  topping out at 2–3 entries (the rates we'd captured by cron
  ticks since the last redeploy).  igold publishes ~1440 sub-day
  observations spanning the last month — binned to one
  last-of-day-UTC sample, that's **30 daily 24K gold prices**
  flowing through the same JSON shape the UI already consumes.

  User-visible effect: the gold/silver bottom sheet now shows
  a full 30-day UAE gold sparkline + populated 30-day stats
  (High/Low/Avg) + populated Recent-Days mini-table for the
  UAE column.  Same fix as v0.32.2 did for silver, now applied
  to gold too — completes the "all four columns have history"
  outcome that was the original v0.23 design intent.

  Karat handling: igold's API does not differentiate `purity`
  parameter values in its output (tested: .999 and .916 return
  identical prices).  24K comes from the API verbatim; 22K is
  derived mathematically as 24K × 22/24 = 91.67%.  Real UAE
  retail 22K runs ~1% above the math because of jewellery
  making charges — the "Rates indicative" disclaimer covers
  that gap.  Source field labels the derivation honestly:
  `"igold.ae (Dubai bullion, 24K AED/gram; 22K derived)"`.

### Fixed
- **Defensive scale normalisation for igold's `data` array.**
  Internal: igold's chart-data endpoint occasionally returns the
  `data` array in milligram scale (silver ~0.009, gold ~0.5)
  while `last_price` is consistently in gram scale.  Probably
  an edge-cache state quirk on their CDN; reproduces
  unpredictably (httpx vs curl behave differently for the same
  request).  Added `_normalise_igold_scale()` which detects the
  scale mismatch by comparing `max(data)` to `last_price` —
  when the ratio is ~1000×, multiplies the array by 1000 to
  bring it back to gram scale.  Either the silver or the gold
  scraper would have silently fallen back to its KT/spot
  fallback path when this happened; now both stay on the igold
  primary regardless of cache state.

### Internal
- Three concurrent scraper changes (gold primary, silver
  normalisation, fallback chains) are kept in `scrapers/gold.py`
  rather than split into per-source files because they share
  the same `_normalise_igold_scale` helper, the same retry
  pattern, and write into the same `PreciousMetalsQuote`.
  Splitting would multiply test fixtures + import paths
  without reducing complexity.

## [0.32.3] — 2026-05-15

### Fixed
- **UAE silver history wasn't actually rendering** despite v0.32.2
  successfully scraping it.  `GoldHistorySheet.kt::ratesForCarat`
  was forcing the UAE silver list to `emptyList()` when carat=Ag
  — a v0.23 hardcoded assumption that "UAE silver has no history,
  ever".  v0.32.2 made that assumption obsolete (igold.ae now
  provides 30 daily AED-per-gram observations) but the UI hadn't
  been told.  Result: the JSON had 30 history points, the sheet
  still showed "spot only" / `—` placeholders on the UAE column.

  Fixes:
  - `ratesForCarat("Ag")` now reads
    `gold.uaeSilver?.history.orEmpty()` instead of hardcoded
    `emptyList()`.
  - Silver trend sparkline gate widened from
    `indiaSilverChrono.size >= 2` to
    `uaeSilverChrono.size >= 2 || indiaSilverChrono.size >= 2`
    so the row renders whenever EITHER side has data.
  - Trend header strings shortened from `"Silver trend · India"`
    to just `"Silver trend"` — the " · India" suffix made sense
    in v0.23 when only the India column had data, misleading now.
  - Empty-state message updated: was "Silver history is
    India-only (UAE shows live spot)" — now reads "Silver
    history is rebuilding — check back tomorrow", consistent
    with the gold variant.  Only shows when BOTH UAE + India
    silver history are empty (igold + LiveChennai both failed —
    rare).

### Internal
- Three concurrent v0.32.x patches were stale in the UI layer
  vs the scraper layer.  v0.32.2 shipped server-side data
  changes but kept the v0.23 client-side assumption that UAE
  silver was spot-only — the kind of bug that's only visible
  end-to-end on a real device with fresh data flowing through.
  Lesson logged: when changing a data source, grep the client
  for `emptyList<...>()` / hardcoded `null` next to that field
  name and confirm nothing's gated on the "this is always empty"
  assumption.

## [0.32.2] — 2026-05-15

### Added
- **UAE silver now has daily history.**  v0.23 through v0.32.1
  rendered the UAE silver card with a "spot only — no daily
  history" footnote because Khaleej Times (our UAE gold source)
  doesn't publish a daily silver page.  v0.32.2 closes that gap
  by switching UAE silver's primary source to
  [`igold.ae`](https://igold.ae/gold-rate) — a Dubai-based
  bullion dealer that publishes AED-denominated silver with a
  30-day daily history via a public chart-data JSON API.

  The new scraper (`scrapers/gold.py::_fetch_igold_uae_silver`):
  - Pulls the monthly endpoint (~1440 sub-day points)
  - Bins to one observation per UTC calendar day (last-of-day
    sample, matching how LiveChennai's silver scraper records
    its history)
  - Returns the standard `SilverSide` shape — drop-in compatible
    with the existing UI

  User-visible effect: the gold/silver bottom sheet now shows a
  real silver trend sparkline + 30-day high/low/avg pills + the
  "Recent days" mini-table populates for the UAE column instead
  of showing `—` placeholders.

### Changed
- UAE silver acquisition path: `igold.ae` is now primary,
  `gold-api.com XAG × AED peg` retained as **fallback** when
  igold is unreachable.  On rare outage days the silver card
  stays populated (no blank state) but the sparkline disappears
  until igold comes back.  Source field labels distinguish the
  two paths ("igold.ae (Dubai bullion, AED/gram)" vs "Spot
  (gold-api.com → AED peg, fallback)") so a user reading the
  bottom-sheet attribution can tell which source they're
  looking at.
- USER_GUIDE.md FAQ updated: removed the "Why is UAE silver
  spot only?" entry (no longer accurate); added a "Where does
  UAE silver come from?" entry describing the primary +
  fallback chain.
- Data Freshness table entry updated from
  `Silver (UAE — gold-api.com XAG × AED peg)` to
  `Silver (UAE — igold.ae AED/gram, gold-api.com fallback)`.

### Internal
- No Android-client networking changes — `charts.igold.ae` is
  contacted only by the scraper running in GitHub Actions; the
  app continues to fetch the already-baked rates.json from
  GitHub Pages, so the allowlist in
  `NetworkSecurity.kt::ALLOWED_HOSTS` and
  `network_security_config.xml` stays untouched.
- UI's "spot only — no daily history" placeholder strings
  (`gold_sheet_uae_spot_only`, `gold_sheet_uae_spot_long`) are
  kept in resources — they remain visible only when igold
  falls back to the spot path and there's no history to draw.
  Graceful degradation rather than dead code.

## [0.32.1] — 2026-05-15

### Fixed
- **Splash logo invisible on OLED black.**  v0.32.0 switched the
  dark splash background to pure `#000000`, but the brand mark
  is a navy `"TR"` + dark refresh arrows on a *transparent*
  background.  Result on AMOLED dark devices: the splash logo
  appeared to render only its teal+yellow accents, with the main
  letterform missing.  Fixed at two layers:
  - **In-app splash + About screen logo** (`TransferRateLogo`
    composable) is now wrapped in a near-white circular "coin"
    backing.  Same pattern the toolbar logo already used since
    v0.29.x — just hoisted into the shared composable so both
    Splash + About inherit it.  Padding scales proportionally
    with `size` (6%) so the coin always has a small white halo
    around the mark.
  - **OS-level Android 12+ splash** (the briefer splash shown
    *before* Compose runs) now declares
    `windowSplashScreenIconBackgroundColor = #FFFFFF` in
    `values-night/themes.xml`.  The system draws the white coin
    automatically — no need to ship a separate dark-mode icon
    drawable.

## [0.32.0] — 2026-05-15

### Changed
- **Dark mode is now OLED-true-black system-wide.**  Was a navy
  stack (background `#0F1720` + surface `#1F2F41` + variant
  `#334462`) anchored on Stripe brand navy.  Real OLED pixels
  rendered the navy as a low-amber glow; pure `#000000`
  turns those pixels off entirely, giving ~30–40% lower power
  draw on AMOLED dashboards and the high-contrast floating-
  content look modern users associate with finance apps.  Cards
  lifted with small +brightness steps (`surface #0E0E0E`,
  `variant #1A1F26`) so cards still pop off the background —
  pure black-on-black loses the edge separation that makes
  hierarchy readable.  Brand-indigo primary stays the same;
  the bigger contrast against true black actually improves
  APCA Lc (L=85 primary on L=0 background = Lc -90 vs the
  previous -64 on navy).  `values-night/colors.xml` added so
  the cold-start splash bg is also `#000000` — no white-flash
  during splash → app transition on OLED dark mode.
  `values-night/themes.xml` `windowLightStatusBar` flipped
  `true → false` so status-bar icons render light on the new
  black bg.

### Fixed
- **`relativeTime()` was returning English regardless of in-app
  language** — "8 minutes ago புதுப்பிக்கப்பட்டது" mixed-language
  was visible in Tamil mode for the mid-market `Updated …` line.
  Rewritten as a `@Composable` that resolves CLDR plurals through
  Android's `<plurals>` resource so each locale picks the correct
  quantity form per its own rules.  New resources:
  `time_just_now` (string) + `time_minutes_ago`,
  `time_hours_ago`, `time_days_ago` (plurals) — translated for
  en/ta/hi/ml with proper `one` / `other` quantity tags.
- **Inline status indicators on the provider-card right column**
  ("stale", "Estimated · awaiting verification") were hardcoded
  English literals.  Extracted to `status_stale_short` and
  `status_estimated` resources; translated for ta/hi/ml.
- **Snapshot grid card titles in the gold/silver bottom sheet**
  (`"🪙  GOLD"` / `"◇  SILVER"`) were hardcoded English even
  though the rest of the sheet was internationalised in v0.30.9.
  Now uses `metals_gold` / `metals_silver` resources and renders
  via `AutoSizeText` so the longer Tamil/Malayalam labels fit.
- **Splash screen text was hardcoded `Color(0xFF0A1F44)` deep
  navy**, which would have been invisible on the new
  OLED-black splash background.  Splash now uses theme-aware
  `MaterialTheme.colorScheme.background` for the fill and
  picks a light text colour when in dark mode.
- **`Updated 8 minutes ago` eyebrow line** converted to
  `AutoSizeText` because the localised Tamil version
  ("8 நிமிடங்கள் முன்பு புதுப்பிக்கப்பட்டது") can reach ~30
  glyphs and overflow on 360 dp phones.
- **`"AED "` amount-input field prefix** was a hardcoded literal;
  extracted to `amount_input_prefix` resource (`translatable=
  "false"` — AED is an ISO currency code, not user-language
  text — but the indirection lets a future locale override it
  if needed).

### Added
- Internationalisation tooling: Android `<plurals>` resources
  per locale with proper `quantity="one"` / `quantity="other"`
  tags.  Android's CLDR runtime now picks the right form
  automatically — no Kotlin-side if-singular-else branching.

### Internal
- Locale key parity verified: en 119 keys, ta/hi/ml 115 keys
  each (the 4 deltas are `translatable="false"` brand
  constants — `app_name`, `badge_best`, `badge_manual`,
  `share_url` — which Android resolves to default automatically).
- 25 `softWrap = false` sites remain in the codebase, all
  vetted as either numeric values (predictable widths) or
  fixed-English unit codes (don't localise).  Every label /
  chrome / user-prose site that previously could clip has now
  been converted to `AutoSizeText`.

## [0.31.1] — 2026-05-15

### Added
- **`AutoSizeText` composable** — new reusable utility in
  `ui/AutoSizeText.kt`.  Shrinks the font size in steps until the
  text fits the available width, then draws.  Hits the
  `TextOverflow.Ellipsis` safety net only if even the minimum
  size doesn't fit.  Implements the standard
  `onTextLayout` → recompose-smaller pattern; typically settles
  in 1–3 layout passes for non-Latin labels, zero for English.

### Fixed
- **Tamil / Malayalam labels no longer ellipsise across the app.**
  v0.30.9 brought full i18n to the gold/silver bottom sheet but
  the home-screen gold card still showed "தங்..." / "வெ..." in
  Tamil and "സ്വർ..." / "വെ..." in Malayalam because the labels
  were rendered at a fixed 10 sp that fit "GOLD" / "SILVER" but
  not the wider non-Latin equivalents.  Static font reduction
  would have hurt English; `AutoSizeText` hits the right size
  per locale.  Applied at every label/chrome site app-wide:
  - **Home screen**: app title "Transfer Rate", `MID-MARKET`
    eyebrow, toolbar chips (AUTO/LIGHT/DARK/REFRESH/SHARE), gold
    card header labels, gold-card failure-state label
  - **Gold/silver bottom sheet**: sheet title (both + gold-only
    variants), "Last 30 days, per gram" subtitle, all three
    trend headers (24K / 22K / Silver · India), "30-day stats ·
    X" / "Recent days · X" headings, "View full N-day history →"
    CTA, sub-sheet title + subtitle, history-table column
    headers (Date / UAE / India)
  - **Pills**: `StatPill` labels (High / Low / Avg), `CaratChip`
    labels (24K / 22K / Silver)
- Numeric values and English-only unit codes (24K, 22K, 1g,
  1kg, AED, ₹) stay as plain `Text` — their widths are
  predictable, no benefit from auto-sizing.

### Changed
- Replaced ~15 instances of
  `maxLines = 1, softWrap = false, overflow = Ellipsis` with
  `AutoSizeText`.  The remaining ~30 `softWrap = false` sites
  are either numeric values (predictable width) or fixed-English
  unit codes (don't localise).

## [0.31.0] — 2026-05-14

### Added
- **Share-best-rate button.**  New share icon in the top app bar
  composes a plain-text summary of today's BEST provider rate
  and fires Android's `ACTION_SEND` chooser so the user can
  forward it to WhatsApp / SMS / email / Telegram / anything
  else that accepts text.  Payload (en):

      🏆 Today's best AED→INR rate

      26.0900 via Aspora
      You'd get ₹78,270 for AED 3,000
      Mid-market: 26.0851

      Compare 11 UAE→India providers: https://imraneggy.github.io/transfer-rate/

  Strings are split into small chunks (`share_title`,
  `share_rate_via_format`, `share_amount_format`,
  `share_midmarket_format`, `share_footer_format`) so translators
  can re-order phrases naturally instead of wrestling positional
  args inside one mega-format.  Translated for en/ta/hi/ml; URL
  is `translatable="false"` (the static GitHub Pages URL is
  language-agnostic).
- **7-day trend arrow on each provider card.**  Compares today's
  rate against the rolling 7-day average from the same history
  data the inline sparkline already consumes:
    - `▲` (green) when today's rate is &gt; 0.1% above the 7-day
      avg — the corridor is moving in the user's favor at this
      provider
    - `▼` (red) when &gt; 0.1% below — provider is offering a
      worse rate than its own recent baseline
    - no glyph (no visual noise) when within the 0.1% flat band
  Threshold matches the existing vs-mid threshold so the two
  indicators are mutually-consistent: a ▲ here roughly
  corresponds in magnitude to a visible "+0.0xxx vs mid" line.
  Glyph renders at 14 sp inline before the 20 sp rate digits,
  baseline-aligned; colour palette reuses the existing
  positive/negative pair from the vs-mid line (no new theme
  entries).  Only rendered when at least 2 history points are
  available — same gate the sparkline uses.

### Changed
- Toolbar action row layout: SHARE is an icon (not a chip)
  because adding a fourth labeled chip would push the title past
  the ellipsis threshold on 360 dp phones in Tamil / Malayalam
  (already tight at 2 chips + 1 icon since v0.30.0).  Share is
  disabled while the app is in `Loading` / `Failed` state and
  re-enables the moment a Ready state arrives.
- `RateView` composable signature now takes `history: List<Double>`
  so the trend arrow can compute the 7-day average without
  re-fetching.  `isDark` / `positive` / `negative` palette
  decisions moved to the outer function scope (was redeclared
  inside the vs-mid block) so the trend arrow and the vs-mid
  line share one palette decision per card.

## [0.30.9] — 2026-05-14

### Added
- **Gold/silver bottom sheet fully internationalised.**  The
  popup that opens when you tap the gold/silver home-screen card
  was the last UI surface still ~90% English regardless of
  in-app language.  Twenty-five new string keys extracted from
  `GoldHistorySheet.kt` and translated for en/ta/hi/ml.
  Coverage:
  - Sheet title (gold-only vs gold + silver variants)
  - "Last 30 days, per gram" subtitle
  - "24K trend" / "22K trend" / "Silver trend · India"
    sparkline headers
  - "spot only" / "spot price only — no daily history"
    UAE-silver placeholders
  - "Silver" carat-chip label (24K / 22K stay English as unit
    codes)
  - "30-day stats · X" / "Recent days · X" stat-table headings
  - "High" / "Low" / "Avg" stat pills
  - "Silver history is India-only..." / "Building history —
    check back tomorrow." empty-state messages
  - "View full N-day history →" call-to-action
  - Full-history sub-sheet title and subtitle
  - "UAE (AED)" / "India (₹)" sub-sheet table headers
  - "Date" column header
  - "Building…" sparkline placeholder
  - "Rates unavailable" on the home-screen card failure state
  - ErrorView headlines + hints + "Try again" button
    ("Can't reach the rate feed" / "Couldn't load rates")
- All translations are first-cut and flagged for native-speaker
  review at the top of each `values-*/strings.xml` file (Tamil,
  Hindi, Malayalam).

### Changed
- Country abbreviations ("UAE" / "INDIA" / "IN"), unit codes
  (24K, 22K, 1g, 1kg, AED, ₹) stay English across all locales by
  design — they're chart-axis-style labels, not prose.  The
  translation principle is documented at the top of each
  locale file.
- Sources line at the bottom of the gold sheet now reuses the
  existing `metals_disclaimer` string (already shown on the
  home-screen gold card), eliminating the duplicate hardcoded
  English copy.
- All extracted `Text` widgets gained
  `overflow = TextOverflow.Ellipsis` so any future locale with
  wider glyph metrics degrades gracefully rather than chopping
  mid-character (continues the defensive sweep started in
  v0.30.7 / v0.30.8).

## [0.30.8] — 2026-05-13

### Fixed
- **Tamil / Hindi / Malayalam no longer clip provider names or
  gold-card headers.**  Switching the in-app language to Tamil (and
  to a lesser extent Malayalam) exposed two cascading clips on
  360 dp phones:
    1. The verbose `vs mid` translation
       (`%1$s மிட் ரேட் ஒப்பீட்டில்` / `%1$s മിഡ് റേറ്റിനെ
       അപേക്ഷിച്ച്`) pushed the provider-card right column to
       ~200 dp, starving the name column to <30 dp and rendering
       "Aspora" as "A..." and "TransferGo" as "Transfe...".
    2. The v0.30.7 `· AED` header suffix overflowed in Tamil
       ("தங்கம் · AED") and Malayalam ("സ്വർണം · AED"), tripping
       the safety-net ellipsis ("தங்..." / "സ്വ...").
  Fixes:
    - `rate_vs_mid_format` shortened to `%1$s vs <transliterated
      mid>` in ta/hi/ml.  "vs" is universally understood across
      UAE-Indian code-switching speech; the `மிட்` / `मिड` /
      `മിഡ്` transliteration of "mid" matches the existing
      `midmarket_eyebrow` convention.  Frees ~30 dp in Hindi,
      ~70 dp in Tamil, ~80 dp in Malayalam — enough to keep the
      name column comfortable.
    - `rate_equals_mid` shortened similarly.
    - `· AED` suffix on gold/silver headers reverted; values
      stay bare numbers (AED is implicit from the adjacent
      MID-MARKET card's `1 AED → INR` subtitle and the ₹ glyph
      on every INR line).  Letter-spacing bumped 0.6 → 0.8 sp
      back toward the v0.30.6 visual without the v0.30.6
      truncation problem.
- **Provider names now allowed to wrap to 2 lines.**  Was
  `maxLines = 1, softWrap = false` since v0.27 — meant the
  fallback was a hard clip even with `Ellipsis` set, because the
  fallback couldn't kick in when the column was too narrow for
  the *first* character + badge.  `maxLines = 2, softWrap = true`
  is the proper defense: long provider names ("Wall Street
  Exchange") or long badges in any locale wrap onto the second
  line instead of being chopped.  Ellipsis still triggers if a
  name overflows two lines, which should never happen in
  practice.

### Changed
- Translation principle for short comparator strings clarified in
  the ta/hi/ml file headers: "vs" stays English on purpose; it is
  a 2-letter universal marker that every UAE-Indian remittance
  speaker already uses in conversation, so translating it adds
  width without adding clarity.

## [0.30.7] — 2026-05-13

### Fixed
- **Silver column no longer clips on 360 dp phones.**  On narrow
  Android devices (Pixel 4a / Galaxy A-series / most Xiaomi at the
  base width) the home-screen gold-vs-silver card was rendering
  `AED 10.06` as `AED 10.0…` with a half-cut digit — the silver
  per-gram and per-kg values, plus the ₹ totals, overflowed the
  ~83 dp column-content budget once padding was deducted.  The
  fix moves the AED unit out of every value and into the column
  header (`🪙 GOLD · AED` / `◇ SILVER · AED`); values render as
  bare numbers (`568`, `10.06`, `1006`), recovering ~28 dp per
  row.  Header letter-spacing tightened from 1.0 sp → 0.6 sp to
  fit the longer label.  `overflow = TextOverflow.Ellipsis` added
  to the header label, the AED value, and the ₹ value as a
  defensive safety net so any future locale rendering wider digits
  (Devanagari, Tamil numerals) tails off with `…` rather than
  being chopped mid-glyph.  ₹ glyph stays as a one-rune prefix on
  the INR line — no change there.
- No string changes; "GOLD" / "SILVER" remain the existing
  `metals_gold` / `metals_silver` resources.  The ` · AED`
  suffix is appended inline because AED is a currency ISO code,
  not user-language text.

## [0.30.6] — 2026-05-14

### Removed
- **LuLu Exchange dropped from the provider list.**  Their F5 BIG-IP
  WAF blocks every datacenter IP we tested — GitHub Actions runners,
  Cloudflare Workers, AWS, Azure, OVH.  The only path that worked
  was a self-hosted runner on a residential IP (`scrape-lulu-
  residential.yml`), and the operational overhead of keeping that
  runner online wasn't worth one provider out of twelve.  The
  remaining eleven providers cover the same UAE→India corridor.
  History preserved in `infra/lulu-proxy/` and
  `infra/lulu-residential/` for future revisitors; if you find a
  cloud path that bypasses LuLu's WAF, the scraper interface is
  unchanged and re-adding takes one line in
  `scrapers/run_all.py`.
- `scrapers/lulu.py`, `scrapers/lulu_browser.py`,
  `scrapers/lulu_inject.py` removed.
- `.github/workflows/scrape-lulu-residential.yml` renamed to
  `.yml.disabled` — preserves the workflow definition without
  letting it fire on the half-hourly cron.

### Changed
- Welcome modal copy updated in en/ta/hi/ml: "Up to twelve
  UAE→India providers" → "Up to eleven", and `LuLu` removed
  from the bullet's provider list.

## [0.30.5] — 2026-05-14

### Changed
- **Provider-card hierarchy re-flipped: rate on top, ₹ amount below.**
  v0.30.0 had made the rupee total the headline; user feedback
  was that in practice you scan the *rate* column first to compare
  providers and only check the rupee total once you've picked the
  winner.  So the layout now reads:
    `25.8400`   (20sp Bold, full contrast)
    `₹ 25,840`  (16sp SemiBold, full contrast)
    `+0.0123 vs mid`  (12sp colored)
  The `@` prefix on the rate is dropped at headline size — bare
  number reads cleaner without the inline-context marker.

### Fixed
- **About page now fully localised in ta/hi/ml.**  v0.30.0–0.30.4
  shipped the section titles and the new Language / Target-alert
  cards translated, but the long-form body paragraphs (mid-market
  explainer, "what is BEST", privacy block, footer disclaimer +
  license line, daily-high body + permission-denied hint) were
  still hardcoded English.  Externalised all 11 paragraphs into
  `about_*_body*` keys and translated into Tamil, Hindi, Malayalam.
- **Provider-history popup now fully localised in ta/hi/ml.**  The
  "Rate history (last 10 days)" header, "No history yet…"
  empty-state copy, "Date · Time" column header, and "Visit X ↗"
  CTA were still hardcoded English.  Added `history_*` and
  `provider_visit_format` keys + ta/hi/ml translations.

### Deferred
- Gold/silver history sheet body text — much larger and lower
  daily usage; scheduled for v0.30.6.

## [0.30.4] — 2026-05-13

### Fixed
- **Per-app language picker now actually opens** — v0.30.2 added the
  in-app "Language" card with a button that fired
  `Settings.ACTION_APP_LOCALE_SETTINGS`, but Android 13+ requires
  apps to declare which locales they support via
  `android:localeConfig` in the manifest *plus* a corresponding
  `res/xml/locales_config.xml` file.  Without that declaration the
  picker either doesn't show at all or opens an empty page that
  appears to do nothing — which is what users saw on v0.30.2 and
  v0.30.3.  Added `xml/locales_config.xml` listing en/ta/hi/ml and
  wired it into the `<application>` tag.  The "Open language
  settings" button now correctly takes the user to a list with
  English, Tamil, Hindi, Malayalam.

## [0.30.3] — 2026-05-13

### Added
- **Hindi (`hi`) localisation** — first cut, native-speaker review
  pending.  Same translation principles as Tamil: brand names and
  `BEST`/`MANUAL` badges stay English, toolbar chips stay compact
  English, body and section text translate, finance/tech terms get
  transliterated rather than fully Hindi-ised (`mid-market` →
  `मिड-मार्केट` rather than the academic `मध्य-बाज़ार`).
- **Malayalam (`ml`) localisation** — first cut, native-speaker
  review pending.  Same principles as Tamil/Hindi.  Finance terms
  transliterated for Kerala-NRI readability (`mid-market` →
  `മിഡ്-മാർക്കറ്റ്` rather than `ഇടത്തരം-വിപണി`).
- The "Language" card in About now lists all four supported
  languages (English, தமிழ், हिन्दी, മലയാളം).

### Changed
- `resourceConfigurations` ship-list expanded from `["en", "ta"]` to
  `["en", "ta", "hi", "ml"]`.  Completes the UAE-India language trio
  for v0.30.x.  Future locales follow the same opt-in pattern.

## [0.30.2] — 2026-05-13

### Added
- **Language shortcut in About** — a "Language" card with a button
  that deep-links into Android's per-app language picker
  (`Settings.ACTION_APP_LOCALE_SETTINGS`).  Lets users switch the
  app's language between English and Tamil without changing the rest
  of their phone.  Falls back to the device-wide language settings
  if the per-app picker is unavailable on a stripped-down OEM ROM.

### Changed
- **Rate text bumped on each provider card** — the per-AED rate
  (`@ 25.8400`) was rendered at `bodySmall` (~12sp, muted) below the
  bold receive amount.  Now `titleSmall` at 16sp SemiBold in the
  full-contrast on-surface colour, so both the rupee total *and* the
  rate are readable at a glance.  Hierarchy preserved by the typographic
  minor-third step (20sp → 16sp → 12sp).

## [0.30.1] — 2026-05-13

### Fixed
- **CI build broke on `v0.30.0` tag** — a leftover private
  `stringResource(id: Int)` shim in `RatesScreen.kt` shadowed the real
  `androidx.compose.ui.res.stringResource` overloads, so the new
  format-arg call sites added in v0.30.0 (`R.string.last_updated`,
  `R.string.rate_vs_mid_format`) failed compilation with
  *"Too many arguments for 'fun stringResource(id: Int): String'"*.
  Removed the shim and imported the real function directly so both
  the single-arg and `vararg formatArgs` overloads resolve. No
  user-visible change; v0.30.1 is the buildable equivalent of
  v0.30.0.

## [0.30.0] — 2026-05-13

### Added
- **Receive-amount as the headline figure** on each provider card.
  Visual hierarchy flipped: the rupee amount the user actually receives
  (`₹ 25,840`) is now the bold headline; the per-AED rate (`@ 25.8400`)
  becomes the small detail line below. Reflects what users actually
  optimise for — *"how much arrives"*, not *"what's the rate"*.
- **Custom rate-target alert** in About → Rate-target alert. Set a
  threshold like `25.85` and the next time any provider's AED→INR rate
  hits or exceeds it, the status bar fires a one-tap notification.
  Independent of the daily-high toggle (a user can want target alerts
  without the noisier "every new high" stream, or vice versa). Per-day
  dedup keyed on (target, local date) so the same target only pings
  once per day. Sane bounds 15.00–40.00.
- **Tamil (`ta`) localisation** — first cut, native-speaker review
  pending. Externalised ~70 user-facing strings into
  `res/values/strings.xml` and added `res/values-ta/strings.xml` with
  Tamil translations. Brand names (`Transfer Rate`, provider names) and
  badge tokens (`BEST`, `MANUAL`) stay English by design (trademarks +
  universal 4-letter markers); body and section text translate, with
  finance/tech terms transliterated rather than fully Tamilised
  (`mid-market` → `மிட்-மார்க்கெட்` rather than the academic
  `சந்தை சராசரி`). UAE-Tamil-diaspora-first for the v0.30 series.

### Changed
- `resourceConfigurations` ship-list expanded from `["en"]` to
  `["en", "ta"]`; future locales (Hindi, Malayalam) follow the same
  pattern.

## [0.29.6] — 2026-05-10

### Fixed
- **Silver vanished from the gold/silver module** when UAE silver
  scraper transiently failed. The Kotlin `silverAvailable` gate
  required BOTH UAE and India sides to be `status: ok` — so a single
  DNS hiccup on the UAE-side spot-XAG fetch threw away the working
  India silver (with 10-day history). Softened to per-side: render
  whichever sides are available, with `—` placeholders for the
  missing one. Applies to both the home `GoldHeader` card and the
  `GoldHistorySheet` popup.
- **Trend-line value placement** — moved from on-curve labels (which
  overlapped the line, clipped on edges, and looked accidental) to a
  small caption row below the chart in the form `↑ 25.84   ↓ 25.71
  • 25.78`. Robinhood / Bloomberg / TradingView pattern. Sparkline
  shape stays clean; values are read as a summary, not guessed from
  position.
- **Light mode still felt washed** despite v0.29.3's primary lift —
  diagnosis: every surface was at L≥87, no visual depth between
  paper-bg, cards, and accents. Deepened `surfaceVariant` L=87 → L=82
  (`#E2EAF3` → `#D1DDE9`) so cards visibly pop. `outline` L=72 → L=66
  for stronger card edges. `outlineVariant` L=85 → L=80 for divider
  presence. `onSurfaceVariant` L=50 → L=47 to clear APCA Body 60 on
  the new darker surfaceVariant. **All 26 text/bg pairs still pass
  APCA** in both light and dark modes (verified via
  `tools/validate_color_palette.py`).
- **BEST card brand tint intensity** boosted: light-mode mix from 22%
  → 32% into white, so the winning provider's brand colour actually
  reads instead of looking milky-pale. Re-ran
  `tools/extract_brand_colors.py --write` to regenerate
  `ProviderBrand.kt`.
- **UAE silver scraper resilience** — added 3-attempt retry with
  exponential backoff (2s, 4s) on the gold-api.com fetch. Production
  was failing on transient DNS resolution errors that cleared within
  seconds; one-shot was too fragile. TODO(v0.30.0): add a true
  second-source fallback (e.g. goldprice.org's public XAU/XAG feed)
  for prolonged outages.

---

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
## [0.29.5] — 2026-05-10

### Fixed
- **History gaps fixed.** `public/history.json` and
  `data/uae_gold_history.json` were being **regenerated every cron
  run but never committed to the repo** — the scrape workflow only
  staged `public/rates.json`. Result: per-provider 7-day sparklines
  and the UAE gold 30-day chart only retained data from the rare
  commits that happened to include them (May 2 and May 9 in
  production — hence the "—" cells the user saw between those
  dates). `.github/workflows/scrape.yml` now also stages and commits
  `public/history.json` and `data/uae_gold_history.json`.
- **Lari Exchange TLS chain fixed.** Lari sends only the leaf cert
  in their handshake (browsers AIA-fetch the missing Sectigo
  intermediate; Python's ssl module doesn't). Captured the Sectigo
  intermediate via the leaf's AIA URL, bundled it with certifi's
  root CA bundle into `scrapers/certs/lari-chain.pem` (137 certs
  total, self-contained, ~280 KB). The lari.py scraper already
  prefers this path when present; verified with httpx `HTTP 200`,
  rate parsed as `25.63 INR/AED`.

### Changed
- **Real provider brand colors** for the BEST card tint, computed
  from the bundled logo PNGs by `tools/extract_brand_colors.py` —
  k-means clusters the most-saturated dominant pixels per logo,
  blends 22% into white for light-mode tint and 45% into the dark
  surface for dark-mode tint. No more hand-picked guesses;
  `ProviderBrand.kt` is now generated from the real assets and
  re-runnable (`python tools/extract_brand_colors.py --write`).
  Captured raw colors:
  Wise `#083500` (dark green), Remitly `#243954` (slate),
  Aspora `#5523B2` (purple), LuLu `#15ABE7` (cyan),
  TransferGo `#FFD000` (yellow), Lari `#F58E2C` (orange),
  Federal `#EC9532` (orange), GCC `#02A451` (green),
  Index `#FDDA26` (yellow), Ahalia `#023B7F` (navy),
  Al Ansari `#112C69` (navy), Al Dahab `#02A12F` (green).

### Added
- **`tools/extract_brand_colors.py`** — re-runnable brand-color
  extraction for `ProviderBrand.kt` whenever a logo PNG changes.
- **`scrapers/certs/lari-chain.pem`** — self-contained TLS bundle
  for Lari Exchange (intermediate + certifi roots).

---

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
## [0.29.4] — 2026-05-10

### Added
- **Numeric value labels along sparkline trends.** The Sparkline
  composable gained a `showLabels` flag (default off) plus a `formatter`
  callback. Enabled on the gold/silver trend sparklines in the gold
  history sheet and the provider full-history sparkline in the
  per-provider sheet — three labels render: at the **min** value
  (below the trough), the **max** value (above the peak), and the
  **last** value (next to the highlight dot). Caller controls
  formatting (AED 0-decimal vs INR thousands-grouped vs four-decimal
  rate). Sparkline height bumped 40→64 dp (gold sheet) / 56→76 dp
  (provider sheet) so labels have room without clipping.

### Changed
- **WelcomeSheet copy genericised** — removed mentions of "Google
  Finance", "Khaleej Times", "LiveChennai", "GitHub Pages",
  "Cloudflare Worker", and "Source code on GitHub" from the
  user-visible bullets. The mid-market bullet now just says "the
  wholesale interbank rate"; the gold/silver bullet says "live UAE
  and India rates"; the privacy bullet talks about
  application-layer outbound restrictions without naming the hosts.

### Known issues
- **Lari Exchange shows `status: error` in production**
  (`ConnectError: SSL CERTIFICATE_VERIFY_FAILED`). The v0.28.0 TLS
  hardening switched `verify=False` to `verify=certifi.where()`,
  but `lariexchange.com` ships an incomplete SSL chain that
  certifi's bundled CAs reject. The fix is to capture Lari's
  intermediate certs (`openssl s_client -connect www.lariexchange.com:443
  -showcerts`) and ship them at `scrapers/certs/lari-chain.pem` —
  the scraper already prefers that path when present. Tracked as a
  separate item; remaining 11 providers continue to update normally.

---

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
## [0.29.3] — 2026-05-10

### Fixed
- **Light-mode palette tightened — no more "washed out and pale".**
  Primary moved from `#8486FF` (L=70) to `#6F73FF` (L=63). Surface
  variant darkened L=92 → L=87 for visible card depth. Outline
  lifted L=80 → L=72.
- **All 13 remaining `colorScheme.outline` text usages corrected** —
  `outline` is the M3 role for divider strokes, not text. Footers,
  table headers, version stamps, "= mid-market" delta labels, and
  similar quiet labels switched to `onSurfaceVariant`. APCA Body
  threshold now passes for all of them in both schemes.
- **Dark-mode `onPrimary` deepened** L=20 → L=12 (Lc 57 fail → 90 pass).
- **Dark-mode `onSurfaceVariant` lifted** L=80 → L=85 (Lc -56 → -64).
- **Dark-mode primary lifted** L=80 → L=85 so it reads legibly when
  used as text on dark surface (AED "Set" trailing button, MID-MARKET
  eyebrow), not just as fill.
- **"View full N-day history" sub-sheet button** converted from
  surface-variant bg + primary text (Lc 50 fail) to filled-button
  style (Lc 90+ pass). Reads more clearly as a CTA simultaneously.

### Changed
- **"Ag" carat chip relabelled to "Silver"** in the gold/silver sheet.
  Internal carat key still `"Ag"`.
- **BEST provider card uses the winning provider's brand tint**
  instead of generic indigo. New `ProviderBrand.kt` maps each known
  provider to light-mode and dark-mode tints chosen so onSurface text
  stays APCA-legible.
- **AED "Set" trailing button** on the Sending input — visible
  affordance to commit + dismiss the keyboard.
- **Logo coin neutral white** in both light and dark mode so the
  brand mark's own navy + teal palette renders correctly.
- **About page sources genericised** — specific upstream-host names
  removed; gold/silver sheet footer also genericised.

### Added
- **`tools/validate_color_palette.py`** — APCA contrast validator
  that programmatically scores every text-on-background pair the app
  renders, plus a swatch-PNG generator. Run it after any palette
  change. Output: `docs/color-validation/{report.md,light-swatches.png,dark-swatches.png}`.

---

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
## [0.29.2] — 2026-05-09

### Added
- **First-launch welcome modal** replacing the small in-list "💡 Welcome"
  hint card. Opens as a `ModalBottomSheet` covering six bullets — live
  remittance rates, mid-market benchmark, gold/silver tracker, refresh
  button, daily-high alerts, privacy posture. Persists dismissal via
  `welcome_dismissed_v2` SharedPreference. Re-triggerable from
  About → "Reset welcome tour" so users can revisit the feature tour.
- **Daily-high status-bar alerts default to ON.** First cold start
  with the toggle on triggers a one-shot `POST_NOTIFICATIONS` system
  prompt. If granted: alerts work. If denied: `dailyHighEnabled` is
  flipped to false so the About switch reflects reality. We never
  re-prompt — Android's "permanently denied" path is harsher UX than
  one polite ask. Track via the new `permission_requested_v1`
  SharedPreference.
- **`docs/LOCALIZATION.md`** — multi-day plan for shipping Tamil,
  Hindi, Malayalam in v0.30.0. Six phases: string externalisation,
  AI + native-speaker translation, resource folder structure, font /
  typography for Indic scripts, per-locale numeric formatting,
  testing matrix.

### Fixed
- **Logo visibility regression in dashboard + About page.** The
  `transfer_rate_logo.png` PNG is dark navy + teal but only ~22%
  opaque (large transparent margins around the brand mark), so on its
  own at 24 dp (toolbar) or 72 dp (About hero) it visually disappears
  against the light page background. Now wrapped in a
  `primaryContainer`-tinted circular coin: 32 dp coin + 28 dp logo in
  the toolbar; 112 dp coin + 96 dp logo on the About hero. Provides a
  defining circular shape and a soft brand-tinted halo without
  competing with surrounding content.
- **About page Privacy section** still mentioned the mosque finder
  and location permission (removed in v0.29.0). Rewritten to reflect
  the actual current state: INTERNET + ACCESS_NETWORK_STATE
  permissions only, plus optional POST_NOTIFICATIONS for daily-high
  alerts, with two-host outbound allowlist enforced at the OkHttp
  layer.

### Changed
- **`fastlane/metadata/android/en-US/full_description.txt`** rewritten
  for v0.29.x reality. Was claiming "seven verified providers"
  (we now ship up to twelve) and "every ~hour" refresh (it's every
  15 minutes). Added the new gold/silver tracking section, the
  daily-high default-on detail, the welcome tour, the 3.4 MB APK
  story, and the AOSP/GrapheneOS compatibility note.
- **README** refreshed for v0.29.1 baseline: latest-release callout
  with the broken-versions warning, accurate provider count, "ABI
  splits are now functionally identical" note (no native code in
  v0.29.x), security/privacy section now reflects the OkHttp
  allowlist enforcement and two-host outbound, fastlane and missing
  dirs corrected in the repo-layout tree.
- `NotificationPrefs.dailyHighEnabled` getter default `false` → `true`.
  Existing installs that explicitly turned the toggle off keep their
  `false` (SharedPreferences only consults the default for absent keys).

---

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
## [0.29.1] — 2026-05-08

### Fixed
- **App-on-launch crash** introduced silently in v0.28.0 and surviving
  through v0.28.x and v0.29.0. The new app-logo pieces (mid-market
  popup avatar in `Avatar.kt`, top app-bar wordmark icon in
  `RatesScreen.kt`) called `painterResource(R.drawable.ic_splash)` —
  but `ic_splash.xml` is a `<bitmap>` XML drawable wrapping
  `@mipmap/ic_splash_image`. Compose's `painterResource()` only accepts
  **vector drawables** (`<vector>`) **or direct raster files**
  (PNG/JPG/WEBP); the `<bitmap>` wrapper throws
  `IllegalArgumentException: Only VectorDrawables and rasterized
  asset types are supported`. The crash fired during the first measure
  pass, before any frame rendered — so the app appeared to "not open".
  Both call sites switched to `R.drawable.transfer_rate_logo` (a real
  PNG in `drawable-nodpi/`) which is what `SplashScreen` was already
  using successfully. `ic_splash.xml` itself is preserved — it's still
  used by the OS-level Android 12+ splash mechanism, which DOES accept
  `<bitmap>` drawables.

### Why this slipped past CI
- Gradle compiled successfully because `painterResource()` is type-safe
  on the resource ID — Kotlin doesn't know whether the resource is a
  vector, bitmap, or raster at compile time.
- `changelog-sync` only validates docs.
- We have no on-device smoke test in CI; the crash only surfaces at
  measure-pass time on a real Android runtime.

---

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
## [0.29.0] — 2026-05-08

### Removed
- **Mosque finder feature removed in its entirety.** The map screen,
  the Overpass + MapLibre stack, the location-permission requests, and
  the toolbar entry point are all gone. The app now does one thing —
  AED→INR remittance + gold/silver rates — and does it more focused.
  - Six source files deleted (~1.4k LoC):
    `data/Mosque.kt`, `data/OverpassService.kt`, `ui/LocationProvider.kt`,
    `ui/MapLibreMapView.kt`, `ui/MosqueScreen.kt`, `ui/MosqueViewModel.kt`.
  - Two Android permissions removed: `ACCESS_FINE_LOCATION` and
    `ACCESS_COARSE_LOCATION`. The app now requests only `INTERNET`,
    `ACCESS_NETWORK_STATE`, and (opt-in) `POST_NOTIFICATIONS`.
  - Two MapLibre dependencies dropped: `maplibre-android-sdk` (11.11.0)
    and `maplibre-android-annotation` (3.0.2). **Universal APK shrinks
    from ~47 MB → ~35 MB** (the native MapLibre `.so` files were the bulk).
  - Two outbound hosts removed from `ALLOWED_HOSTS` and the
    `network_security_config.xml` domain block: `overpass-api.de` and
    `tile.openstreetmap.org`. The app now only ever speaks to GitHub
    Pages and the Cloudflare Worker.
- The `RatesScreen` toolbar 🕌 button is gone; `MainActivity` no longer
  routes to `MosqueScreen`.
- README "Tech stack" table and `docs/USER_GUIDE.md` had their
  Mosque-finder / MapLibre rows removed. Play Store description
  (`fastlane/.../full_description.txt`) updated to reflect the
  single-permission posture.

### Privacy posture change
- **No more location permission visible in Play Store / Android
  Settings.** Existing v0.28.x installs that had granted location will
  see the permission disappear automatically on update — Android
  uninstalls a permission whose declaration is removed from the
  manifest. Nothing the user needs to do.

---

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
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

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
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

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
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

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
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

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
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

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
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

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
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

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
## [0.24.0] — 2026-05-07

### Added
- Silver columns in the gold/silver bottom sheet alongside gold. UAE
  silver is spot-only (no daily history); India silver has 10-day
  history from LiveChennai.

---

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
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

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
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

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
## [0.21.0] — 2026-05-07

### Fixed
- **Defeated all CDN caching on rates fetch.** Added a `?_t=<currentMillis>`
  cache-bust query parameter to every `RatesRepository` request after
  observing 3+ minute staleness from GitHub Pages' `Cache-Control: max-age=600`
  combined with Fastly edge cache.

---

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
## [0.20.0] — 2026-05-07

### Removed
- "Today's Best" in-app banner from the home dashboard. Status-bar
  notification (added in v0.19) covers the same need without consuming
  screen real estate.

---

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
## [0.19.0] — 2026-05-06

### Added
- **Opt-in status-bar notifications for new daily highs.** WorkManager
  periodic worker compares the current best rate to today's running
  high; a notification fires when a new high is set. User opts in from
  the About screen; channel uses `IMPORTANCE_DEFAULT`.

---

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
## [0.18.0] — 2026-05-06

### Added
- "Today's Best" banner above the rates list, showing the
  current-best provider plus today's running peak. (Reverted in v0.20
  in favour of notifications-only.)

---

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
## [0.17.0] — 2026-05-06

### Changed
- **App rebranded from "Exchangia" to "Transfer Rate".** New logo,
  wordmark, splash, palette, package name remains `com.transferrate.app`.

---

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
## [0.16.x] — 2026-05-04

- **0.16.3** — New launcher icon (Stair-Step E variant).
- **0.16.2** — User location marker on the mosque map; per-pin distance
  badge.
- **0.16.1** — Mosque finder UX cleanup (filter chips, smoother camera).
- **0.16.0** — Smoother MapLibre camera, location accuracy improvements,
  Lari (Georgia) provider logo refresh.

---

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
## [0.15.0] — 2026-05-04

### Added
- **Mosque finder.** New screen with MapLibre + OSM raster tiles
  (no API key, $0-ops). 12th provider added.

---

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
## [0.14.0] — 2026-05-03

### Added
- 22K gold trend in addition to 24K.
- Mid-market rate history sparkline.
- Typography pass: tightened tracking, bumped headline weights.

---

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
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

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
## [0.12.2] — 2026-05-03

### Fixed
- Dropped `?attr/colorControlNormal` from the launcher icon (no
  AppCompat theme available in the icon's render target).

---

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
## [0.11.0] — 2026-05-02

### Changed
- Rebrand to **Exchangia** — typography, icon, and palette
  (subsequently rebranded again to Transfer Rate in v0.17).

---

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
## [0.10.x] — 2026-05-02

- **0.10.1** — Ahalia Exchange (11th provider).
- **0.10.0** — TransferGo provider; redesigned icon, splash, theme polish.

---

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
## [0.9.x] — 2026-05-02

- **0.9.0 (later)** — Provider history sheet; provider logos; 10-day
  retention.
- **0.9.0** — WorkManager prefetch worker; distribution-ready release
  builds (signing, ABI splits, ProGuard rules).

---

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
## [0.8.x] — 2026-05-02

- **0.8.1** — Rate history sparklines; improved logo graphics.
- **0.8.0** — Variable amount input; pull-to-refresh; on-disk cache;
  better error states.

---

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
## [0.7.0] — 2026-05-02

### Added
- Three-bar logo and branded splash.

### Removed
- Unverified provider stubs trimmed from the rates list.

---

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
## [0.6.0] — 2026-05-02

### Changed
- App scoped to the **AED → INR corridor only** (was multi-corridor).

---

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
## [0.5.x] — 2026-05-02

- **0.5.1** — Minimalist T-monogram icon and splash screen.
- **0.5.0** — Manual rate-entry admin UI (`/admin/`) for app-only
  providers (e&, Botim, Comera, Careem Pay).

---

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
## [0.4.0] — 2026-05-02

### Added
- Google Finance mid-market rate as the benchmark/header.
- Light/dark mode toggle.
- New launcher icon.
- Receive-amount estimates per provider.

---

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
## [0.3.0] — 2026-05-02

### Added
- App icon (initial design).
- Mid-market rate header.
- Ten provider stubs (most marked `investigating`).

---

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
## [0.1.0] — 2026-05-01

### Added
- Initial scaffold: Python scrapers, Android 14+ Compose app, GitHub
  Actions CI, project documentation.

---

## [Unreleased]

### Added
- **Orient Exchange scraper.** Added a public JSON-backed provider for AED -> INR rates using `https://www.orientexchange.com/Orient/GetExchangeRates`, with fixture-backed parser tests and live scrape validation.

### Documentation
- Updated provider documentation to reflect the active scraper registry and the June 2026 Dubai/Abu Dhabi provider gap check.
[0.29.6]: https://github.com/imraneggy/transfer-rate/releases/tag/v0.29.6
[0.29.5]: https://github.com/imraneggy/transfer-rate/releases/tag/v0.29.5
[0.29.4]: https://github.com/imraneggy/transfer-rate/releases/tag/v0.29.4
[0.29.3]: https://github.com/imraneggy/transfer-rate/releases/tag/v0.29.3
[0.29.2]: https://github.com/imraneggy/transfer-rate/releases/tag/v0.29.2
[0.29.1]: https://github.com/imraneggy/transfer-rate/releases/tag/v0.29.1
[0.29.0]: https://github.com/imraneggy/transfer-rate/releases/tag/v0.29.0
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
