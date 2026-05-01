# Security policy

## Supported versions

| Component       | Supported version |
|-----------------|-------------------|
| Android app     | latest release    |
| Scraper backend | always `main`     |

## Reporting a vulnerability

Please do not open a public GitHub issue for security problems.

Email the maintainer instead. Include:

* A description of the issue
* Steps to reproduce
* Affected commit / app version
* Your contact info if you'd like a credit

You can expect:

* Acknowledgement within 72 hours
* Fix or mitigation plan within 14 days for confirmed issues
* Public disclosure coordinated with you

## What counts as a vulnerability for this project

* Anything that lets a third party feed a malicious `rates.json` into the
  app pipeline (e.g. compromise of the GitHub Actions workflow)
* Code execution paths in the app from untrusted JSON input
* Bypass of the network security allowlist
* Leakage of any user data (note: the app intentionally collects none —
  if you find it does, that itself is the vulnerability)

## Hardening choices already in place

* GitHub Actions workflows pin third-party actions by full commit SHA.
* `permissions: {}` at workflow root denies everything by default; only
  the commit step receives `contents: write`.
* Android Network Security Config restricts connections to
  `imraneggy.github.io`. No cleartext.
* `usesCleartextTraffic="false"` on the `<application>` element.
* `allowBackup="false"` and explicit `data_extraction_rules.xml`.
* App requests only `INTERNET`.
* JSON parser configured strictly (`isLenient=false`); document is
  bound-checked before being rendered.
* R8 full-mode strips debug-level Log calls in release builds.

## Out of scope

* Vulnerabilities in third-party provider websites we scrape (report those
  to the provider directly).
* Network-level attacks against the user's device that are not specific
  to this app.
