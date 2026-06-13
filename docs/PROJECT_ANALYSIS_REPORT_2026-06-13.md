# Transfer Rate Project Analysis Report

Date: 2026-06-13 (Asia/Dubai)
Repository: imraneggy/transfer-rate
Branch reviewed: main

## Current Status

The Android debug deployment validation is green.

- Debug workflow: android-test-build
- Successful run: 27440694778
- Run URL: https://github.com/imraneggy/transfer-rate/actions/runs/27440694778
- Commit tested: 9efa534a7f86c918801c8f8b0ec8f08c8a568407
- Artifact: transfer-rate-debug-apk
- Artifact ID: 7601741548
- Artifact size: 41,831,071 bytes

A CI configuration blocker was fixed:

- Commit: 9efa534a7f86c918801c8f8b0ec8f08c8a568407
- Fix: CI debug builds no longer fail just because release-signing secrets are absent.
- Root cause: android/app/build.gradle.kts evaluated the release signing error during Gradle configuration even when the requested task was :app:assembleDebug.

A data-publishing workflow blocker was fixed:

- Commit: 721f87de1afb370f22b6bd20b641393c52864edc
- Fix: .github/workflows/scrape.yml now uses git diff --cached --quiet to detect staged generated artifacts.
- Root cause: git status --porcelain --cached is invalid with the current Git version, so fresh Pages data deployed but generated JSON was not committed back to main.
- Verification: scrape run 27441082256 succeeded and created commit 0dfef2a43d39a75a9f3321addb4ab6e5dfa28827.
- main public/rates.json after fix: completed_at=2026-06-12T20:26:51Z, INR providers ok=12.

## Release Completion

v0.32.6 has been released.

- Tag: v0.32.6
- Tag commit: e5b6c502627fa2a750a3b6d6eb4e84eb1c43eb68
- Changelog workflow: 27450258278 passed
- Android release workflow: 27450258280 passed
- Release URL: https://github.com/imraneggy/transfer-rate/releases/tag/v0.32.6
- Release APK assets:
  - app-arm64-v8a-release.apk
  - app-armeabi-v7a-release.apk
  - app-universal-release.apk
  - app-x86_64-release.apk

Frontend design work can now continue from a green release baseline.
## Architecture Summary

Transfer Rate has two main planes:

1. Scrape/data plane
   - Python scrapers run in GitHub Actions.
   - Output is public/rates.json and public/history.json.
   - GitHub Pages serves static data to the app.
   - The scrape workflow commits generated artifacts back to main for audit history.

2. Android presentation plane
   - Android 14+ Kotlin/Compose app.
   - OkHttp fetches GitHub Pages JSON with cache busting.
   - Host allowlist is enforced in app code through HostAllowlistInterceptor.
   - Refresh can call a Cloudflare Worker, which triggers workflow_dispatch without exposing the GitHub PAT in the APK.

The design is strong for zero-cost hosting, public data, low operational burden, and simple user privacy.

## Strengths

- Good security posture for a small public app: pinned Actions, scoped workflow permissions, app-level outbound host allowlist, no analytics, no accounts.
- Clear separation between scraper output and Android UI.
- Android debug build now has a dedicated non-release workflow and APK artifact.
- Release-signing secrets exist in GitHub Actions: KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD, REFRESH_TRIGGER_SECRET.
- Production Pages data is current and all 12 INR providers are currently ok.
- FreshnessBanner feature addresses a real product risk: users need a visible warning when rates are delayed or stale.

## Gaps And Risks

### Python scraper tests fixed

The stale gold scraper tests were updated to match the current source architecture.

- UAE gold test now covers igold.ae primary JSON parsing and 22K derivation.
- India gold test now covers LiveChennai history parsing.
- New fixture: tests/fixtures/livechennai_gold_silver.html.
- Verification: test.yml run 27449778191 passed on commit 759e8f10ce600e6bd794cd2ac862a5d8fd514fa0.

### Android test coverage gap

The repo currently validates Android compilation, but not Compose behavior.

Recommended additions:

- Add lightweight unit tests for freshnessState thresholds if moved from private top-level functions into testable internal code.
- Add Compose UI tests for FreshnessBanner states: fresh hidden, delayed visible, critical visible, invalid timestamp visible, refresh button calls callback.
- Add a release dry-run workflow that runs assembleRelease only on workflow_dispatch before tagging.

### Workflow reliability

- Node 20 deprecation warnings appear in Actions logs. Pinned action SHAs should be reviewed and upgraded to Node 24-compatible revisions before September 2026.
- Add a post-scrape check that asserts generated artifacts were committed or intentionally unchanged.

## Completed Release Plan For v0.32.6

Completed:

1. android-test-build passed for the version bump.
2. Android versionName was bumped to 0.32.6 and versionCode to 68.
5. Update CHANGELOG.md with v0.32.6 entries.
6. Regenerate/update docs/CHANGELOG.html if required by the changelog workflow.
7. Update docs/FRONTEND_UPDATE_REPORT.html with final release validation.
8. Create tag v0.32.6 only after the above is green.
9. Verify android-build release APK artifacts are attached to the GitHub Release.

## Prioritized Improvement Backlog

1. Add Android FreshnessBanner tests.
2. Add release dry-run workflow for assembleRelease without publishing a GitHub Release.
3. Add release dry-run workflow for assembleRelease without publishing a GitHub Release.
4. Add workflow validation for scrape artifact commit consistency.
5. Update action pins to Node 24-compatible revisions.
6. Add a small status badge for android-test-build in README.
7. Add screenshots for the new stale/delayed banner states.
8. Consider moving long Claude context backups out of main or into a separate archive branch if repo size/history noise becomes a problem.

## Handoff For Next Agent

Start here:

1. Continue with frontend design work from the v0.32.6 baseline.
2. Keep test.yml and android-test-build.yml green before the next release.
3. Add FreshnessBanner UI tests and a release dry-run workflow next.

Python pytest is green as of run 27450166895 and v0.32.6 release artifacts are verified.
## Provider Expansion Update - 2026-06-13

Orient Exchange has been added as a live provider after the Dubai/Abu Dhabi provider gap check.

- Implementation commit: c9885b15162ea23401195e2be93c9df4d3e45dd7
- Scraper: scrapers/orient_exchange.py
- Fixture: tests/fixtures/orient_exchange_rates.json
- Registry: scrapers/run_all.py now includes OrientExchangeProvider after Index Exchange.
- Unit tests: test.yml push run 27460858686 passed on the Orient commit; workflow_dispatch run 27460875026 also passed.
- Live scrape: scrape.yml push run 27460858678 passed and public/rates.json now includes provider_id=orient_exchange with status=ok, rate=25.7599, and provider timestamp 13 Jun 2026 08.20 AM.

June 2026 UAE provider gap check result:

1. Orient Exchange: added now; clean public JSON endpoint.
2. Al Ghurair Exchange: promising, but direct endpoint calls need exact request matching before it can be trusted.
3. Wall Street Exchange: public converter appears to be cash FX and was stale in the fetched page; not a remittance-rate source yet.
4. Sharaf Exchange, Al Rostamani, Al Fardan, Joyalukkas: no confirmed free, stable AED->INR public endpoint yet.

Next data-plane task: investigate Al Ghurair's frontend request pattern and add it only if the GitHub Actions runner can fetch it reliably without secrets, CAPTCHA, or browser-only state.
## v0.32.7 Release Preparation - 2026-06-13

Prepared patch release v0.32.7 for the Android amount-input and Orient provider UI work.

- Android versionCode: 69
- Android versionName: 0.32.7
- Includes Orient Exchange scraper and live `public/rates.json` validation.
- Includes Orient Android avatar asset and brand tint mapping.
- Includes Sending amount live-update behavior and quick amount chips: 500, 1,000, 4,000, 6,000, 10,000.

Pre-tag validation required before creating `v0.32.7`:

1. `test.yml` on latest main.
2. `scrape.yml` on latest main.
3. `android-test-build.yml` on latest main.

After those pass, create tag `v0.32.7` and verify `changelog-sync` plus `android-build` release APK assets.
## v0.32.7 Release Completion - 2026-06-13

v0.32.7 has been released.

- Tag: v0.32.7
- Tag commit: 3e8fc00c3e60a61b2389bfd5806024c47df75f29
- Release URL: https://github.com/imraneggy/transfer-rate/releases/tag/v0.32.7
- Pre-tag validation:
  - test.yml: 27461445131 passed
  - scrape.yml: 27461445543 passed
  - android-test-build.yml: 27461445966 passed
- Tag workflows:
  - changelog.yml: 27461573956 passed
  - android-build.yml: 27461573946 passed
- Release APK assets:
  - app-arm64-v8a-release.apk (3,462,958 bytes)
  - app-armeabi-v7a-release.apk (3,460,116 bytes)
  - app-universal-release.apk (3,513,031 bytes)
  - app-x86_64-release.apk (3,463,619 bytes)

The earlier changelog run 27461505573 failed because docs/CHANGELOG.html did not yet contain the v0.32.7 section. The rendered HTML changelog was fixed in commit 3e8fc00c3e60a61b2389bfd5806024c47df75f29, the tag was moved to that fixed commit, and the corrected tag workflows passed.