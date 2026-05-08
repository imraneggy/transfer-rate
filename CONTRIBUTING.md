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

## Release checklist (maintainers)

Every `v*.*.*` tag push runs the `changelog-sync` workflow, which **fails
the build** if either `CHANGELOG.md` or `docs/CHANGELOG.html` is missing
an entry for the new version. The intent is to catch forgotten doc
updates before the APK ships.

Before tagging:

1. Bump `versionCode` (+1) and `versionName` (semver) in
   [`android/app/build.gradle.kts`](android/app/build.gradle.kts).
2. Add a new `## [X.Y.Z] — YYYY-MM-DD` section at the top of
   [`CHANGELOG.md`](CHANGELOG.md) under one or more of the
   *Added / Changed / Fixed / Removed / Infrastructure* headings.
3. Add a matching `<section class="release" id="vX-Y-Z">` block at the
   top of [`docs/CHANGELOG.html`](docs/CHANGELOG.html), and a
   corresponding `<li><a href="#vX-Y-Z">` entry in the `<nav class="toc">`.
   Move the `current` CSS class from the previous release section onto
   the new one. Update the "Latest:" pill in the hero header.
4. Update the link reference at the bottom of `CHANGELOG.md`:
   `[X.Y.Z]: https://github.com/imraneggy/transfer-rate/releases/tag/vX.Y.Z`
5. Refresh user-visible docs that reference the version: e.g.,
   [`docs/USER_GUIDE.md`](docs/USER_GUIDE.md) "Last updated for…" line.
6. Commit with the standard subject style:
   `feat: <one-line summary> (vX.Y.Z)`.
7. Push to `main`. Once CI is green, tag and push:
   ```bash
   git tag vX.Y.Z
   git push origin vX.Y.Z
   ```
8. The `android-build` workflow produces signed APKs and the
   `changelog-sync` workflow validates the docs. Both must succeed
   before the GitHub Release is published.

If `changelog-sync` fails, fix the missing doc entry, push the fix,
delete the tag locally + remote, and re-tag:

```bash
git tag -d vX.Y.Z
git push --delete origin vX.Y.Z
# fix docs, commit, push
git tag vX.Y.Z
git push origin vX.Y.Z
```

## Disclosure

Found a security issue? See [`SECURITY.md`](SECURITY.md). Do not file public
issues for vulnerabilities.
