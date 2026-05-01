# Contributing

Thanks for considering a contribution. The most common change is adding or
fixing a provider scraper. This guide walks through that.

## Ground rules

1. **Public data only.** Do not authenticate, do not replay private mobile
   APIs, do not bypass paywalls or rate limits.
2. **Conservative parsing.** A scraper that surfaces a wrong number is worse
   than one that surfaces no number. Prefer raising an error to guessing.
3. **Polite traffic.** One request per scrape; identify yourself via the
   shared User-Agent in `scrapers/utils.py`.
4. **No new dependencies** in `requirements.txt` without discussion. Each
   added dependency is a supply-chain liability.
5. **All PRs include a brief test note** — paste the local output of
   `python -m scrapers.<your_scraper>` on a recent run.

## Adding a provider scraper

1. Create `scrapers/<provider>.py`. Use `scrapers/wise.py` as a template:
   - `id` (slug, lowercase) and `display_name`
   - `fetch_inr(amount_aed)` returns a `Quote`
2. Import and register it in `scrapers/run_all.py` (`PROVIDERS` list).
3. Run locally:
   ```bash
   python -m scrapers.run_all
   cat public/rates.json
   ```
4. Confirm your provider's row has `status="ok"` and a sane rate.
5. Open a PR against `main`. The CI workflow (`scrape.yml`) will exercise
   your scraper on the merge.

### Allowed sources

* Public web pages displaying rates (no login needed)
* Public APIs documented for marketing/widget use
* RSS/JSON feeds the provider publishes

### Disallowed sources

* Authenticated endpoints (even if you have an account)
* Reverse-engineered mobile APIs that require captured tokens or apk-
  extracted secrets
* Anything behind an explicit "Terms of Service" prohibiting scraping
  (read it before adding)

## Reporting a bug or wrong rate

Open a GitHub issue with:
* Provider name
* What rate the app shows vs. what the provider's site shows
* Time observed (UTC)
* Screenshot of both, if possible

Wrong-rate issues are P0 — we patch and force a fresh scrape ASAP.

## Code style

* Python: 4-space indent, type hints, no print debugging in committed code
* Kotlin: official style (already enforced via `kotlin.code.style=official`)
* No comments that restate the code; comments explain *why*.

## Disclosure

Found a security issue? See [`SECURITY.md`](SECURITY.md). Do not file public
issues for vulnerabilities.
