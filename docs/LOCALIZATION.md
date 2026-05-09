# Localization plan — Tamil, Hindi, Malayalam

Target: ship Transfer Rate in **Tamil (`ta`)**, **Hindi (`hi`)**, and
**Malayalam (`ml`)** alongside English (`en`) as **v0.30.0**. The natural
audience for an UAE→India remittance app is Indian-diaspora workers in
the UAE; the three languages above cover the dominant southern (Tamil,
Malayalam) and northern (Hindi) Indian-language groups represented in
the UAE workforce.

This document is the v0.30.0 work breakdown. It is NOT yet implemented;
the v0.29.x line ships English-only.

---

## Phase 1 — String externalization (~half-day)

### Audit current state

The codebase has **mixed string handling**: a small fraction is in
`res/values/strings.xml` (`R.string.app_name`, `R.string.app_tagline`,
`R.string.disclaimer`); the rest is hardcoded in Kotlin source as
either string literals or interpolated templates.

```bash
# Hardcoded English strings to externalize:
grep -rn '"[A-Z][a-zA-Z ]\+"' android/app/src/main/java/com/transferrate/app/ui \
  | grep -vE 'fontFamily|providerId|fontFeatureSettings|=\s*"[a-z_]+"$'
# (filter out short identifiers, font names, JSON keys)
```

Expected pull: 200–300 user-visible English strings across the UI
files, especially:
- `RatesScreen.kt` — dashboard labels, BEST/MANUAL badges, "vs
  mid-market", "You receive", "Sending", quick-pick chip labels
- `WelcomeSheet` content (~600 words across 6 bullets)
- `AboutScreen.kt` — section headings, body copy, privacy paragraph,
  permission-denied hint
- `GoldHistorySheet.kt` — "30-day stats", "Recent days", "View full
  history", "spot only", attribution text
- `ProviderHistorySheet.kt` — "Rate history (last 10 days)"
- `MidMarketHeader` — "MID-MARKET" eyebrow, "Updated X minutes ago"
- `SplashScreen.kt` — tagline (already in strings.xml ✓)
- Notification body templates in `NotificationCenter.kt`

### String key naming convention

```xml
<!-- res/values/strings.xml -->
<string name="dashboard_best_badge">BEST</string>
<string name="dashboard_manual_badge">MANUAL</string>
<string name="dashboard_you_receive">You receive</string>
<string name="dashboard_sending">Sending</string>
<string name="dashboard_vs_midmarket">%1$s vs mid-market</string>

<string name="welcome_title">Welcome to Transfer Rate</string>
<string name="welcome_subtitle">A free, open-source comparison app...</string>
<string name="welcome_bullet_rates_title">Live remittance rates</string>
<string name="welcome_bullet_rates_body">Up to twelve UAE→India...</string>
<!-- ...etc, prefix-namespaced by screen -->

<string name="about_section_privacy">Privacy</string>
<string name="about_privacy_p1">This app collects nothing...</string>
<string name="about_privacy_p2">Outbound HTTPS is allowlisted...</string>
```

Naming pattern: `<screen>_<section>_<purpose>`, with `%1$s`/`%2$d`
positional placeholders (NOT named) so translators can reorder freely
without breaking type-safety.

### Plurals

Use `<plurals>` for any count-driven copy:

```xml
<plurals name="full_history_button">
    <item quantity="one">View full %d-day history →</item>
    <item quantity="other">View full %d-day history →</item>
</plurals>
```

Tamil and Malayalam each have a distinct plural form for `one` vs
`other`; Hindi shares English's "other-only" pattern in informal usage
but technically distinguishes `one`. Use ICU's plural rules — Android's
`getQuantityString()` handles this correctly per locale.

---

## Phase 2 — Translation (~1–2 days; depends on quality bar)

### Translation strategy: AI-first, native-speaker-reviewed

Three tiers of strings, with different trust levels:

1. **High-stakes / financial** ("BEST", "vs mid-market", "You receive",
   "MID-MARKET"): translated by AI, then reviewed by a native speaker
   the maintainer trusts. Rate-related labels MUST be unambiguous.

2. **Onboarding / about copy** (welcome bullets, privacy paragraph):
   AI-translated, light review. These sections accept some stylistic
   imperfection.

3. **Boilerplate** ("Cancel", "OK", "Settings"): pull from Android's
   own `android.R.string.*` system catalogue when possible — already
   correctly localized for every supported language.

### Glossary

The following loanwords / technical terms should remain in English
even in Indic-language strings (UAE-Indian users use them in their
own conversations):

| English | Tamil | Hindi | Malayalam |
|---------|-------|-------|-----------|
| AED | AED | AED | AED |
| INR | INR / ₹ | INR / ₹ | INR / ₹ |
| Mid-market | mid-market (transliterate) | mid-market | mid-market |
| Refresh | புதுப்பி (or transliterate "Refresh") | रिफ्रेश | റിഫ്രഷ് |
| Provider | வழங்குநர் | प्रदाता | ദാതാവ് |
| 24K / 22K | 24K / 22K | 24K / 22K | 24K / 22K |

### Translator credit + provenance

Add a `docs/translators.md` listing the human reviewers for each
language so contributors can be credited and re-engaged when copy
changes. Auto-translated strings without human review are marked
`@string/auto_translated_review_pending` in the code review notes.

---

## Phase 3 — Resource folder structure

```
android/app/src/main/res/
├── values/                  ← English (default)
│   └── strings.xml
├── values-ta/               ← Tamil
│   └── strings.xml
├── values-hi/               ← Hindi
│   └── strings.xml
└── values-ml/               ← Malayalam
    └── strings.xml
```

Android picks the locale folder automatically based on the device's
language setting. No code changes needed past externalization.

### Build configuration

`android/app/build.gradle.kts` currently restricts bundled locales to
English to keep the APK small:

```kotlin
resourceConfigurations += listOf("en")
```

Change to:

```kotlin
resourceConfigurations += listOf("en", "ta", "hi", "ml")
```

APK size impact: ~50–100 KB per language (string tables only — no
glyph data, since fonts handle scripts at render time).

### Per-locale Play Store listing

`fastlane/metadata/android/` will need:

```
fastlane/metadata/android/
├── en-US/        ← exists
├── ta-IN/        ← Tamil (India regional code)
├── hi-IN/        ← Hindi
└── ml-IN/        ← Malayalam
```

Each contains the same `full_description.txt`, `short_description.txt`,
`changelogs/<versionCode>.txt`, and screenshot folders. Play Store
shows the matching language to users browsing in that locale.

---

## Phase 4 — Font / typography considerations

### Indic-script rendering

Manrope (the bundled body font) has **excellent Latin coverage but no
glyphs for Devanagari (Hindi), Tamil, or Malayalam**. When a string in
those scripts is rendered with `fontFamily = Manrope`, Android falls
back to the system default font for the missing glyphs. Result: mixed
typography per character — Latin numerals in Manrope, Indic letters in
the system Roboto/Noto.

**Acceptable approach (Phase 4a — ship in v0.30.0):** rely on the
fallback. The system fonts on Android 14+ are Noto Sans Devanagari,
Noto Sans Tamil, Noto Sans Malayalam — all professional-quality.
Visual inconsistency between scripts is mild and most users don't
notice once they're reading content.

**Polish path (Phase 4b — v0.31+):** bundle a single multi-script font
that covers all four scripts in a coherent type system. **Noto Sans**
(Adobe / Google) is the industry-standard pan-script family with
Devanagari, Tamil, and Malayalam variants. Cost: +~600 KB per font
weight in the APK. Not worth it for v0.30.0; revisit if users complain.

### Right-to-left

None of the four target languages are RTL. No layout-mirroring work
needed. Defer the Arabic (`ar`) localization decision; Arabic IS RTL
and triggers a substantial layout audit when added.

### Letter-spacing in display sizes

`Type.kt` uses negative letter-spacing on display sizes (e.g.
`displayLarge` letter-spacing -1.4 sp). Devanagari, Tamil, and
Malayalam are sensitive to letter-spacing (compound glyphs assemble
from multiple sub-glyphs). **Audit display-size text in Indic locales
on a real device before shipping.** The numeric "hero rate" rendering
is unaffected (digits are Latin-shaped even in Indic locales).

---

## Phase 5 — Per-locale numeric formatting

`%,.0f` and `%,.4f` format specifiers in Kotlin use the JVM default
locale, which means in `hi-IN` and `ta-IN` users may see numbers
formatted with the **Indian comma grouping** (1,00,000 instead of
100,000). This is correct and culturally expected — don't fight it.

Decimal separator changes in some Indic locales (e.g. Hindi uses `.`
in modern usage; Tamil/Malayalam similar). Test once on a real device.

The `tnum` font feature (tabular figures) we use throughout the
dashboard is locale-independent — same digit widths in every script.

---

## Phase 6 — Testing matrix

For each of `en`, `ta`, `hi`, `ml`:

- [ ] Home dashboard renders with no clipped/overflowing labels
- [ ] WelcomeSheet bullets readable end-to-end
- [ ] AboutScreen privacy paragraph wraps correctly
- [ ] GoldHistorySheet section headings + table headers fit
- [ ] Notification body uses correct language
- [ ] BEST / MANUAL badges visible (consider keeping these as English
      acronyms — they read as "branding" to non-English speakers, like
      "OK" or "PIN")
- [ ] System back-stack labels (window title) match locale

Run on a clean Android 14 emulator with each system language set; no
need for user-facing language picker (Android handles it).

### Per-app language picker (optional)

Android 13+ supports `LocaleManagerCompat.setApplicationLocales()` for
in-app language override. Worth adding to the About screen as a
convenience (some users keep their phone in English but want the
remittance app in their native language for clarity). Single-line
change once externalization is done.

---

## v0.30.0 release checklist (when ready)

1. ☐ Externalize all hardcoded strings into `res/values/strings.xml`
   (Phase 1)
2. ☐ Generate AI-translated `values-ta/strings.xml`,
   `values-hi/strings.xml`, `values-ml/strings.xml` (Phase 2)
3. ☐ Native-speaker review of Tier-1 strings (Phase 2 review)
4. ☐ Update `resourceConfigurations` in `build.gradle.kts` (Phase 3)
5. ☐ Add `fastlane/metadata/android/{ta-IN,hi-IN,ml-IN}/` listings
   (Phase 3)
6. ☐ On-device testing for each locale (Phase 6)
7. ☐ Per-app language picker on About screen (Phase 6 optional)
8. ☐ Update CHANGELOG.md, docs/CHANGELOG.html, README, USER_GUIDE for
   v0.30.0
9. ☐ Tag v0.30.0 and ship

Estimated total effort: **2–3 days** for a high-quality release; **1
day** for an AI-only translation that ships without native review (use
in-app feedback link to collect corrections from users).

---

## Why these three languages first

UAE expat workforce demographics (rough estimates from public sources):

| Language | UAE Indian-diaspora share | Sending corridor |
|----------|---------------------------|------------------|
| Hindi | ~30% (largest) | Mostly North India / NCR |
| Malayalam | ~25% (Kerala diaspora is concentrated in UAE) | Kerala |
| Tamil | ~15% | Tamil Nadu |
| Telugu | ~10% | Andhra / Telangana |
| Marathi | ~5% | Maharashtra |
| Other Indian | ~15% | Various |

Adding Telugu, Marathi, Kannada, Bengali in v0.31+ is a copy-paste of
the v0.30.0 procedure with the specific translations swapped in. The
hard work is in Phase 1 (externalization); each subsequent language is
~1 day of translation + review.
