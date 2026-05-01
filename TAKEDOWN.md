# Takedown / removal request

If you represent a remittance provider listed in this project and you would
like your service removed, here is the process. We respect such requests and
act within 24 hours.

## How to request

Open a GitHub issue titled `Takedown request: <provider>`, **or** email the
maintainer directly. Include:

1. Your name and role at the provider
2. Confirmation that you are authorised to make this request
3. The exact provider entry to remove (or modify)
4. Any supporting context (e.g. ToS section, brand-protection concern)

We do not require a formal legal notice. A clear request from a verifiable
provider contact is enough to start removal.

## What "removal" means

* The provider is removed from `scrapers/run_all.py` so it stops being
  scraped.
* The provider is removed from `public/rates.json` on the next run.
* Historical commits remain in git history, but no current build references
  the provider.
* The Android app, which fetches the live JSON, stops showing the provider
  as soon as users open the app and the JSON refreshes.

## Modification, rather than removal

If you'd like the listing to *exist* but display differently — corrected
name, official deep link to your product, brand styling — open a regular
issue and we'll happily collaborate.

## Brand and trademarks

Provider names are used nominatively to identify the source of the rate.
We do not use provider logos, registered marks, or other branded assets in
the app. If we have inadvertently included any, please flag it and we will
remove immediately.
