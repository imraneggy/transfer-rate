# Continuation Guide — Transfer Rate on a fresh machine

Written 2026-05-15 by Imran + Claude before handover of the original
development machine to Ali & Sons IT.  Captures everything needed to
resume Transfer Rate development end-to-end without losing context.

---

## 1. What the project is, in one paragraph

Transfer Rate is an Android remittance-rate comparison app for the
UAE → India corridor.  Scrapers (Python, GitHub Actions, 15-minute cron)
pull live rates from 11 providers + gold + silver every 15 minutes,
publish a single `rates.json` to GitHub Pages, and the Compose-based
Android client renders the comparison.  Refresh button taps fire a
Cloudflare Worker which dispatches an on-demand `workflow_dispatch` so
users can pull truly-fresh rates without waiting for the next cron tick.
The repo is `github.com/imraneggy/transfer-rate`, public, owner-operated.
Latest released version at handover: **v0.32.5** (2026-05-15).

## 2. Recent shipping history (what happened today)

| Version | What landed |
|---|---|
| **v0.32.5** | Pipeline-merge fix — `_merge_uae_gold_history` was overwriting igold's 30 days with the 2-entry rolling file |
| **v0.32.4** | UAE gold swapped to igold.ae primary (KT fallback). 30-day gold history end-to-end |
| **v0.32.3** | Wire UAE silver history through `ratesForCarat` — v0.23 hardcoded `emptyList()` was eating the new history |
| **v0.32.2** | UAE silver swapped to igold.ae primary (gold-api.com fallback). 30-day silver history end-to-end |
| **v0.32.1** | Splash logo white-coin wrap so navy "TR" stays visible on OLED black |
| **v0.32.0** | OLED-black dark mode + full i18n audit (relativeTime localised, `<plurals>` for time strings, hardcoded stale/Estimated extracted) |
| **v0.31.1** | `AutoSizeText` composable — labels shrink instead of ellipsising in Tamil/Malayalam |
| **v0.31.0** | Share-best-rate button + 7-day trend arrow on provider cards |

Earlier history (v0.30.x and below) is in `CHANGELOG.md` at repo root.

## 3. Setting up Claude Code on the new machine

```bash
# 1. Install Claude Code (per claude.ai/code instructions for the OS)
#    Currently macOS + Windows desktop apps + a CLI.

# 2. Clone the repo
git clone https://github.com/imraneggy/transfer-rate.git
cd transfer-rate

# 3. Restore Claude memory.  The project-id in the path is a hash of
#    the working directory, so it'll be different on the new machine.
#    Find it by running `claude` once in the cloned directory, then
#    cancelling out — the directory is created on first launch.

# macOS / Linux:
PROJECT_DIR=$(ls -d ~/.claude/projects/*transfer-rate* | head -1)
mkdir -p "$PROJECT_DIR/memory"
cp -r .claude-context/memory/* "$PROJECT_DIR/memory/"

# Windows (PowerShell):
$projectDir = Get-ChildItem "$env:USERPROFILE\.claude\projects" -Filter "*transfer-rate*" | Select-Object -First 1
New-Item -ItemType Directory -Force -Path "$($projectDir.FullName)\memory"
Copy-Item ".claude-context\memory\*" -Destination "$($projectDir.FullName)\memory\"
```

After this, when you start a new session in this directory, Claude will
read `MEMORY.md` automatically and surface the project context + your
preferences (terse responses, design-iteration pattern, etc.).

## 4. Git credentials

GitHub auth is per-machine — the Windows Credential Manager entries
don't transfer.  On the new machine, the first `git push` against
`origin` will prompt for credentials.  Easiest path:

```bash
# Option A: PAT via credential helper
git config --global credential.helper store   # or 'manager' on Windows
# Then push once; you'll be prompted for username + PAT.

# Option B: gh CLI auth (recommended)
gh auth login
# Walks through OAuth in a browser; gh stores the token and
# `git push` uses it transparently.
```

If creating a fresh PAT: https://github.com/settings/personal-access-tokens
— needs `contents: write` scope on `imraneggy/transfer-rate`.

## 5. Android development setup (optional)

The CI builds APKs automatically on tag push.  You only need a local
Android setup if you want to side-load debug builds without going
through CI.

```bash
# Install Android Studio (Hedgehog or newer)
# Open the project at android/
# Sync Gradle, then Build → Generate Signed Bundle / APK
# Or via CLI:
cd android
./gradlew assembleRelease
# APK at app/build/outputs/apk/release/app-universal-release.apk
```

For signed release builds you'd need the keystore — it's NOT in the
repo, only in GitHub Actions secrets (`KEYSTORE_BASE64` +
`KEYSTORE_PASSWORD` + `KEY_ALIAS` + `KEY_PASSWORD`).  CI is the
authoritative signing path; local builds fall back to debug signing.

## 6. The roadmap / what's queued

When we left off, the menu was:

**Discussed but paused:**
- **iOS port.**  Three paths weighed (SwiftUI / KMP / PWA).  Recommended
  SwiftUI for premium feel, but needs a Mac + Apple Developer Program
  ($99/year).  No code written yet — decision pending.

**Open features (not started):**
- Home-screen widget
- Multi-corridor expansion (AED → PKR / PHP / BDT / EGP / LKR)
- Spread alerts (notify when provider beats mid-market by > X bps)
- "Send via" deep-links from provider cards
- Holiday awareness ("rates may be stale — UAE bank holiday")

**Quality / debt:**
- Native-speaker review of ta/hi/ml first-cut translations
- Play Store metadata in `fastlane/metadata/android/{ta-IN,hi-IN,ml-IN}/`
- Integration test that fetches live rates.json + asserts all 4
  metal columns have ≥ 7 history points (would have caught the
  v0.32.3/.4/.5 bug chain at once)
- Defensive Compose audit: `softWrap = false` is a footgun

## 7. Where things live (quick reference)

| What | Location |
|---|---|
| Android app source | `android/app/src/main/...` |
| Python scrapers | `scrapers/*.py` |
| Cron schedule | `.github/workflows/scrape.yml` |
| Release build | `.github/workflows/android-build.yml` |
| Storage cleanup | `.github/workflows/prune-release-apks.yml` (v0.32.0 — auto-trims old APKs) |
| Published rates.json | `https://imraneggy.github.io/transfer-rate/rates.json` |
| Cloudflare Worker source | `infra/cloudflare-worker/` |
| User-facing changelog | `docs/CHANGELOG.html` (rendered from `CHANGELOG.md`) |
| Network allowlist | `android/app/src/main/res/xml/network_security_config.xml` + `NetworkSecurity.kt` |

## 8. The "watch for" list (lessons learned during v0.30.x → v0.32.x)

These bit us at least once each — worth grepping the next time you
touch the relevant subsystem:

1. **Data-source change without grepping for hardcoded `emptyList()` /
   `null` at the consumer.**  v0.32.2 + .3 + .5 were the same bug at
   different layers.  When swapping a field's source from "always
   empty" to "may contain data", grep every place doing
   `side["history"] = ...`, `emptyList<...>()`, `?: emptyList()`,
   etc. and verify they respect the new source.
2. **`softWrap = false` + `overflow = Ellipsis` clips mid-glyph** when
   the column is narrower than the first character + a sibling element.
   Use `AutoSizeText` for any label/chrome text; the old combo is
   reserved for fixed-width chips/badges.
3. **Brand assets that depend on a specific background colour**
   become invisible when the colour changes.  The OLED-black switch
   in v0.32.0 hid the navy "TR" logo until v0.32.1 wrapped it in a
   white coin.  Same lesson for any future themed surface.
4. **Tag-push concurrency wedge** — back-to-back tag pushes within
   minutes can wedge the Actions concurrency arbiter.  Recovery:
   delete + re-push the tag at the same SHA.
5. **Compose `stringResource` shadowing** — a private top-level
   `stringResource(id: Int)` shim shadows Compose's vararg overload
   and breaks format-args calls at compile time.  Don't add shims;
   use the import.

## 9. Restoring session JSONLs (optional, for forensics)

The `.claude-context/sessions/*.jsonl.gz` files are the full
conversation transcripts from May 8 → May 15.  They're not needed
for resumption (memory + CHANGELOG capture the load-bearing context)
but useful if you ever want to reconstruct "why did we decide X".

```bash
gunzip -k .claude-context/sessions/41920dff-*.jsonl.gz
gunzip -k .claude-context/sessions/76d9bf25-*.jsonl.gz
# Then read with `jq` or any text editor.  They're ~50 MB uncompressed.
```

## 10. Sanity checklist for "did the handover work?"

After cloning + restoring on the new machine:

- [ ] `git remote -v` shows `origin = https://github.com/imraneggy/transfer-rate.git`
- [ ] `git log --oneline -5` shows v0.32.5 at the top
- [ ] `ls ~/.claude/projects/*transfer-rate*/memory/` shows 9 markdown files
- [ ] `claude` opens a session and `MEMORY.md` contents appear in the
      system context (you'll see the project notes referenced)
- [ ] `git push` works (auth set up)
- [ ] Side-load https://github.com/imraneggy/transfer-rate/releases/latest/download/app-universal-release.apk
      to the personal Android — opens to home screen, shows current rates

If all six check, you're back where you were.

---

That's the handover.  Memory + this guide + the CHANGELOG should give
both you and Claude enough context to pick up the iOS port discussion
or any other roadmap item without missing a beat.  Best of luck.
