"""Shared HTTP utilities for all scrapers.

Centralises:
  - the polite User-Agent + identifying contact (so providers can
    reach the maintainer if they want to ask us to stop scraping)
  - timeouts (connect / read / pool)
  - retry on transient errors (connect timeout, network blip, 502/503)
    with exponential backoff

Per-scraper customisations layer on top by passing extra `headers=`
or wrapping the returned client.
"""
from __future__ import annotations

import time
from typing import Optional

import httpx

USER_AGENT = (
    "transfer-rate-bot/1.0 "
    "(+https://github.com/imraneggy/transfer-rate; open-source UAE remittance "
    "rate aggregator; contact via GitHub issues)"
)

DEFAULT_TIMEOUT = 15.0  # seconds (per-call)


def http_client(timeout: float = DEFAULT_TIMEOUT) -> httpx.Client:
    """Return a configured httpx.Client with polite headers + sane timeouts.

    Use as a context manager so connections close cleanly:

        with http_client() as c:
            r = c.get(url)

    For retry behaviour, prefer get_with_retry() which builds on top
    of this client.
    """
    return httpx.Client(
        timeout=httpx.Timeout(connect=timeout, read=timeout, write=10.0, pool=timeout),
        headers={
            "User-Agent": USER_AGENT,
            "Accept": "application/json, text/html;q=0.9, */*;q=0.8",
            "Accept-Language": "en-US,en;q=0.9",
        },
        follow_redirects=True,
    )


# Errors that warrant a retry (transient network/server issues). These
# differ from response-shape failures (which we do NOT retry — those are
# real schema bugs, not flakes).
_RETRYABLE_EXCEPTIONS = (
    httpx.ConnectTimeout,
    httpx.ReadTimeout,
    httpx.RemoteProtocolError,
    httpx.NetworkError,
)
_RETRYABLE_STATUS = {502, 503, 504, 429}


def get_with_retry(
    url: str,
    *,
    headers: Optional[dict] = None,
    timeout: float = DEFAULT_TIMEOUT,
    max_attempts: int = 3,
    initial_delay: float = 0.5,
) -> httpx.Response:
    """GET with exponential backoff on transient errors.

    Strategy:
      attempt 1 → fail → sleep 0.5s
      attempt 2 → fail → sleep 1.0s
      attempt 3 → either succeeds or the final exception bubbles up

    Total worst-case time: ~3 * timeout + 0.5 + 1.0 = under a minute.
    """
    last_exc: Optional[Exception] = None
    delay = initial_delay
    for attempt in range(1, max_attempts + 1):
        try:
            with http_client(timeout=timeout) as c:
                r = c.get(url, headers=headers or {})
                if r.status_code in _RETRYABLE_STATUS and attempt < max_attempts:
                    time.sleep(delay)
                    delay *= 2
                    continue
                r.raise_for_status()
                return r
        except _RETRYABLE_EXCEPTIONS as exc:
            last_exc = exc
            if attempt >= max_attempts:
                raise
            time.sleep(delay)
            delay *= 2
    # Defensive — loop should have either returned or raised.
    raise RuntimeError(
        f"GET {url} failed after {max_attempts} attempts. Last: {last_exc}"
    )
