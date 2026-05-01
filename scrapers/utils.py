from __future__ import annotations

import httpx

USER_AGENT = (
    "transfer-rate-bot/1.0 "
    "(+https://github.com/imraneggy/transfer-rate; open-source UAE remittance "
    "rate aggregator; contact via GitHub issues)"
)

DEFAULT_TIMEOUT = 15.0  # seconds


def http_client(timeout: float = DEFAULT_TIMEOUT) -> httpx.Client:
    """Return a configured httpx.Client with a polite User-Agent.

    Use as a context manager so connections close cleanly:

        with http_client() as c:
            r = c.get(url)
    """
    return httpx.Client(
        timeout=timeout,
        headers={
            "User-Agent": USER_AGENT,
            "Accept": "application/json, text/html;q=0.9, */*;q=0.8",
            "Accept-Language": "en-US,en;q=0.9",
        },
        follow_redirects=True,
    )
