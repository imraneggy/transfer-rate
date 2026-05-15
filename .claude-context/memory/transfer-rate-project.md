---
name: Transfer Rate app project context
description: imraneggy/transfer-rate Android remittance app — design preferences, architecture, and shipping cadence
type: project
originSessionId: 76d9bf25-7e54-4c08-a667-1570a1f76389
---
**Repo:** github.com/imraneggy/transfer-rate (private). UAE→India remittance rate comparison Android app with gold/silver rates.

**Architecture:**
- Android Compose (Material 3, minSdk 34, Kotlin 1.9+)
- GitHub Actions scrapers publish `rates.json` to GitHub Pages (build_type=workflow)
- Cloudflare Worker at `https://transfer-rate-refresh.imranbatchait.workers.dev` proxies refresh-button taps to a `workflow_dispatch` (PAT lives in Worker env, not in APK)
- Data sources: Khaleej Times (UAE gold), LiveChennai (India gold + silver, 10-day history), gold-api.com XAG × AED peg (UAE silver — spot only, no history), Google Finance (mid-market)

**Current version (2026-05-08):** v0.26.0 just shipped with Stripe Atlas Premium palette (OKLCh-derived from `#635BFF` + `#0A2540` + `#F6F9FC`). User immediately asked to swap the primary — too dark.

**Why:** owner-operated app; design is iterated by feel rather than user testing.
**How to apply:** ship in small increments, confirm visual direction with the user before writing code, version-bump on every release (versionCode + versionName).
