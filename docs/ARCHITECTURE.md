# Architecture

## Goals

* **Free to operate forever** — zero cloud bills, zero credit card.
* **Resilient to one-provider failure** — one bad scraper does not stop
  the others or break the app.
* **Honest about uncertainty** — partial data, stale data, and unknown
  data are first-class concepts in the JSON schema and the UI.
* **No personal data, ever** — the app is read-only against a public
  bulletin board.

## Two planes, one contract

```
                  scrape plane                   presentation plane
   ┌────────────────────────────────────┐    ┌──────────────────────────┐
   │ GitHub Actions cron */15           │    │ Android app              │
   │  ├─ scrapers/run_all.py            │    │  ├─ MainActivity         │
   │  │   └─ ThreadPoolExecutor         │    │  ├─ RatesViewModel       │
   │  ├─ scrapers/{wise,remitly,...}.py │    │  ├─ RatesRepository      │
   │  └─ writes public/rates.json       │    │  └─ Compose UI           │
   └────────────┬───────────────────────┘    └────────────┬─────────────┘
                │                                          │
                ▼                                          ▼
        commits to git                            HTTPS GET, no cache
                │                                          │
                └────────► public/rates.json ◄─────────────┘
                            (GitHub Pages CDN)
```

The single contract between planes is `public/rates.json`. Schema is in
`scrapers/base.py::Quote` (Python) and `data/Rates.kt::ProviderQuote`
(Kotlin). Both sides validate the schema_version field.

## Why GitHub Actions + Pages instead of a server

* Cron-on-Actions for public repos is effectively unlimited.
* Pages is served via GitHub's CDN (Fastly), so traffic from millions of
  users hits cache, not us.
* Every change to rates is a git commit — so we get free history and
  diffs ("why did LuLu's rate jump 0.5%?" is a `git blame` away).
* No secrets to leak. No DNS to manage. No databases to back up.

## Failure model

| What can fail                | What happens                                               |
|------------------------------|------------------------------------------------------------|
| One provider's site is down  | That provider becomes `status="stale"` in JSON; app shows last good rate with warning. |
| One scraper's parser breaks  | `status="error"`; orchestrator preserves previous value as `stale` if available. |
| All scrapers fail in one run | run_all exits non-zero; existing rates.json remains; cron retries in 15 min. |
| GitHub Actions outage        | rates.json keeps the last successful values; app keeps showing them. Status pages would still load. |
| GitHub Pages outage          | App fails to fetch; UI shows "Couldn't load rates. Pull to retry." |
| Android app TLS pin breaks   | We don't pin (cert rotation kills pins). Domain allowlist is enforced instead. |

## Concurrency in run_all.py

Scrapers run concurrently in a `ThreadPoolExecutor`. Each is wrapped so
exceptions become `status="error"` Quote records — never propagate to
caller. Per-provider hard timeout is 25 seconds; total run is bounded by
the GitHub Actions job timeout (5 minutes).

## Atomic write of rates.json

The orchestrator writes to `rates.json.tmp` then `os.replace()` to the
final name. This means readers (the next workflow step doing the git
commit, or a developer reading the file mid-run) never see a half-written
document.

## App data flow

```
Compose UI ──► RatesViewModel.refresh() ──► RatesRepository.fetch()
   ▲                  │                            │
   │                  ▼                            ▼
   │           StateFlow<RatesUiState>     OkHttp ─► JSON ─► validate()
   └────── collectAsStateWithLifecycle ◄───────────┘
```

* `RatesRepository.fetch()` runs on `Dispatchers.IO`, returns
  `Result<RatesDocument>`.
* `RatesViewModel` exposes a sealed `RatesUiState` for explicit
  Loading / Ready / Failed handling — no booleans-and-flags soup.
* `validate()` enforces sanity bounds (no negative rates, no NaN, no
  rates above 1000 INR/AED, no fees above 100k AED).

## Why no database/cache in the app

The data is small (a few KB), public, and changes infrequently. Caching
adds invalidation complexity and storage that has to be encrypted. A
fresh fetch on each session is simpler and respects the user's
expectation that "open app → see latest rate."
