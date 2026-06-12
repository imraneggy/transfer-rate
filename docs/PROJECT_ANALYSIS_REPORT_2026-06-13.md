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

### Release blocker: Python scraper tests are failing

Latest inspected pytest run: 25921673777.

Observed failures:

- test_uae_gold_extracts_24k_and_22k has unexpected igold API requests not registered in pytest-httpx.
- test_india_gold_extracts_history_series expects the older BankBazaar path while the scraper now uses newer gold-source behavior.

Impact: Do not create v0.32.6 until scraper tests are updated and green.

Recommended fix:

- Update tests/test_scrapers.py fixtures and mocks to match current scrapers/gold.py behavior.
- Add explicit mocks for igold chart-data calls.
- Replace stale BankBazaar expectations with the current India gold source or mark BankBazaar as fallback-only if that is the intended architecture.

### Android test coverage gap

The repo currently validates Android compilation, but not Compose behavior.

Recommended additions:

- Add lightweight unit tests for freshnessState thresholds if moved from private top-level functions into testable internal code.
- Add Compose UI tests for FreshnessBanner states: fresh hidden, delayed visible, critical visible, invalid timestamp visible, refresh button calls callback.
- Add a release dry-run workflow that runs assembleRelease only on workflow_dispatch before tagging.

### Workflow reliability

- Node 20 deprecation warnings appear in Actions logs. Pinned action SHAs should be reviewed and upgraded to Node 24-compatible revisions before September 2026.
- Add a post-scrape check that asserts generated artifacts were committed or intentionally unchanged.

## Release Plan For v0.32.6

Before tagging:

1. Fix scraper pytest failures.
2. Run test.yml successfully.
3. Confirm android-test-build still succeeds after version bump.
4. Bump Android versionName from 0.32.5 to 0.32.6 and increment versionCode from 67 to 68.
5. Update CHANGELOG.md with v0.32.6 entries.
6. Regenerate/update docs/CHANGELOG.html if required by the changelog workflow.
7. Update docs/FRONTEND_UPDATE_REPORT.html with final release validation.
8. Create tag v0.32.6 only after the above is green.
9. Verify android-build release APK artifacts are attached to the GitHub Release.

## Prioritized Improvement Backlog

1. Fix scraper tests and make test.yml green.
2. Add Android FreshnessBanner tests.
3. Add release dry-run workflow for assembleRelease without publishing a GitHub Release.
4. Add workflow validation for scrape artifact commit consistency.
5. Update action pins to Node 24-compatible revisions.
6. Add a small status badge for android-test-build in README.
7. Add screenshots for the new stale/delayed banner states.
8. Consider moving long Claude context backups out of main or into a separate archive branch if repo size/history noise becomes a problem.

## Handoff For Next Agent

Start here:

1. Inspect tests/test_scrapers.py around gold tests.
2. Inspect scrapers/gold.py and align tests to current source order.
3. Run or dispatch test.yml after fixing fixtures.
4. Only after pytest is green, prepare the v0.32.6 version/changelog commit.
5. Re-run android-test-build.
6. Tag v0.32.6 and verify android-build release APK artifacts.

Do not create the official release tag while pytest is red.