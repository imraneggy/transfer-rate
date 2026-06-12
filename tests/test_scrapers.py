"""Unit tests for each provider scraper.

We mock the HTTP layer with pytest-httpx, serving captured response
fixtures (committed under tests/fixtures/). Each test verifies:
  1. The scraper's response shape produces a Quote with status='ok'.
  2. The rate value matches what the fixture contains.
  3. Required Quote fields (provider_id, base, quote, fetched_at) are set.

When a provider's site changes its response shape, the regex/JSON path
no longer matches the fixture and the test fails. The test then needs
to be updated alongside the scraper. This is the desired behaviour —
the test is the contract.
"""
from __future__ import annotations

import json
import re

import pytest

from scrapers.ahalia import AhaliaProvider
from scrapers.aspora import AsporaProvider
from scrapers.al_ansari import AlAnsariProvider
from scrapers.al_dahab import AlDahabProvider
from scrapers.federal_exchange import FederalExchangeProvider
from scrapers.gold import fetch_uae_gold, fetch_india_gold
from scrapers.gcc_exchange import GccExchangeProvider
from scrapers.index_exchange import IndexExchangeProvider
# LuluProvider removed in v0.30.6 — F5 BIG-IP WAF blocks every
# cloud datacenter IP we tested, the scraper was deleted.  Tests
# preserved as documentation just below (commented out) until the
# next time test_scrapers.py is reorganised.
from scrapers.mid_market import MidMarketProvider
from scrapers.remitly import RemitlyProvider
from scrapers.transfergo import TransferGoProvider
from scrapers.wise import WiseProvider

from .conftest import fixture_text


# --- Wise ---------------------------------------------------------------

def test_wise_returns_ok_rate(httpx_mock):
    httpx_mock.add_response(
        url=re.compile(r"https://wise\.com/rates/live\b.*"),
        text=fixture_text("wise_rates_live.json"),
        headers={"content-type": "application/json"},
    )
    q = WiseProvider().fetch(target_currency="INR", amount_base=1000.0)
    assert q.status == "ok"
    assert q.provider_id == "wise"
    assert q.rate == 25.8041
    assert q.base == "AED"
    assert q.quote == "INR"
    assert q.received_quote == pytest.approx(25804.1)


# --- Aspora -------------------------------------------------------------

def test_aspora_returns_ok_rate(httpx_mock):
    # Aspora cycles through a host list — match any
    httpx_mock.add_response(
        url=re.compile(r"https://api-z[1-4]\.aspora\.com/forex/rates"),
        text=fixture_text("aspora_forex_rates.json"),
        headers={"content-type": "application/json"},
    )
    q = AsporaProvider().fetch()
    assert q.status == "ok"
    assert q.provider_id == "aspora"
    assert q.rate == 25.81
    assert q.fee_base == 0.0


def test_aspora_rejects_unsupported_target():
    with pytest.raises(RuntimeError, match="only AED->INR"):
        AsporaProvider().fetch(target_currency="USD")


# --- Remitly ------------------------------------------------------------

def test_remitly_returns_ok_with_promo(httpx_mock):
    httpx_mock.add_response(
        url=re.compile(r"https://www\.remitly\.com/ae/en/india.*"),
        text=fixture_text("remitly_page.html"),
        headers={"content-type": "text/html"},
    )
    q = RemitlyProvider().fetch()
    assert q.status == "ok"
    assert q.rate == 25.77  # everydayRate
    assert q.promo_rate == 25.95  # effectiveRate (promo)
    assert q.promo_note is not None
    assert "3500" in q.promo_note
    assert q.fee_base == 0.0


# --- Index Exchange -----------------------------------------------------

def test_index_exchange_extracts_rate_from_html(httpx_mock):
    httpx_mock.add_response(
        url=re.compile(r"https://www\.indexexchange\.ae.*"),
        text=fixture_text("index_exchange_page.html"),
        headers={"content-type": "text/html"},
    )
    q = IndexExchangeProvider().fetch()
    assert q.status == "ok"
    assert q.rate == 25.673940949936


# --- LuLu (removed in v0.30.6) ------------------------------------------
# Provider dropped from the lineup because LuLu's F5 BIG-IP WAF blocks
# every cloud datacenter IP we tested (GitHub Actions runners,
# Cloudflare Workers, AWS, Azure, OVH).  Tests removed in v0.32.2 — the
# scraper module no longer exists.

# --- Al Ansari ----------------------------------------------------------

def test_al_ansari_two_step_with_nonce(httpx_mock):
    # Step 1: homepage GET returns the embedded CC_Ajax_Object
    httpx_mock.add_response(
        url="https://alansariexchange.com/",
        text=fixture_text("al_ansari_home.html"),
        headers={"content-type": "text/html"},
    )
    # Step 2: POST to admin-ajax.php returns the rate
    httpx_mock.add_response(
        url="https://alansariexchange.com/wp-admin/admin-ajax.php",
        method="POST",
        text=fixture_text("al_ansari_ajax.json"),
        headers={"content-type": "application/json"},
    )
    q = AlAnsariProvider().fetch()
    assert q.status == "ok"
    assert q.rate == 25.7069


# --- Al Dahab Exchange --------------------------------------------------

def test_al_dahab_extracts_rate_from_marquee(httpx_mock):
    httpx_mock.add_response(
        url=re.compile(r"https://aldahabexchange\.ae.*"),
        text=fixture_text("al_dahab_page.html"),
        headers={"content-type": "text/html"},
    )
    q = AlDahabProvider().fetch()
    assert q.status == "ok"
    assert q.rate == 25.79


def test_al_dahab_handles_missing_corridor(httpx_mock):
    httpx_mock.add_response(
        url=re.compile(r"https://aldahabexchange\.ae.*"),
        text=fixture_text("al_dahab_page.html"),
        headers={"content-type": "text/html"},
    )
    with pytest.raises(RuntimeError, match="not in marquee"):
        AlDahabProvider().fetch(target_currency="EUR")


# --- Ahalia Exchange ----------------------------------------------------

def test_ahalia_extracts_rate_from_cc_data(httpx_mock):
    httpx_mock.add_response(
        url=re.compile(r"https://ahaliaexchange\.com.*"),
        text=fixture_text("ahalia_page.html"),
        headers={"content-type": "text/html"},
    )
    q = AhaliaProvider().fetch()
    assert q.status == "ok"
    assert q.provider_id == "ahalia"
    assert q.rate == 25.67  # cc_data value, NOT the table 24.55
    assert q.base == "AED"
    assert q.quote == "INR"


def test_ahalia_supports_php_via_phpr_key(httpx_mock):
    httpx_mock.add_response(
        url=re.compile(r"https://ahaliaexchange\.com.*"),
        text=fixture_text("ahalia_page.html"),
        headers={"content-type": "text/html"},
    )
    q = AhaliaProvider().fetch(target_currency="PHP")
    assert q.status == "ok"
    assert q.rate == 16.68


def test_ahalia_rejects_unknown_currency():
    with pytest.raises(RuntimeError, match="no cc_data key"):
        AhaliaProvider().fetch(target_currency="XYZ")


# --- TransferGo ---------------------------------------------------------

def test_transfergo_returns_ok_rate(httpx_mock):
    httpx_mock.add_response(
        url=re.compile(r"https://my\.transfergo\.com/api/fx-rates.*"),
        text=fixture_text("transfergo_rates.json"),
        headers={"content-type": "application/json"},
    )
    q = TransferGoProvider().fetch()
    assert q.status == "ok"
    assert q.provider_id == "transfergo"
    assert q.rate == 25.84357
    assert q.base == "AED"
    assert q.quote == "INR"


# --- Federal Exchange ---------------------------------------------------

def test_federal_exchange_extracts_rate_from_card(httpx_mock):
    httpx_mock.add_response(
        url=re.compile(r"https://www\.federalexchange\.ae.*"),
        text=fixture_text("federal_exchange_page.html"),
        headers={"content-type": "text/html"},
    )
    q = FederalExchangeProvider().fetch()
    assert q.status == "ok"
    assert q.provider_id == "federal_exchange"
    assert q.rate == 25.77
    assert q.base == "AED"
    assert q.quote == "INR"


def test_federal_exchange_rejects_unknown_currency():
    with pytest.raises(RuntimeError, match="no known label"):
        FederalExchangeProvider().fetch(target_currency="XYZ")


# --- GCC Exchange -------------------------------------------------------

def test_gcc_inverts_rate_correctly(httpx_mock):
    """GCC stores AED-per-target; scraper must invert to target-per-AED."""
    httpx_mock.add_response(
        url=re.compile(r"https://www\.gccexchange\.com/media/.*"),
        text=fixture_text("gcc_rates.json"),
        headers={"content-type": "application/json"},
    )
    q = GccExchangeProvider().fetch()
    assert q.status == "ok"
    # Fixture has INR ExchangeRate=0.038744 (AED-per-INR), inverted to
    # ~25.81 INR-per-AED.
    assert q.rate == pytest.approx(1 / 0.038744)


# --- Mid-Market ---------------------------------------------------------

def test_mid_market_uses_google_first(httpx_mock):
    httpx_mock.add_response(
        url=re.compile(r"https://www\.google\.com/finance/quote/AED-INR.*"),
        text=fixture_text("google_finance_aed_inr.html"),
        headers={"content-type": "text/html"},
    )
    q = MidMarketProvider().fetch(target_currency="INR")
    assert q.status == "ok"
    assert q.rate == 25.83844
    assert "Google Finance" in (q.note or "")


def test_mid_market_falls_back_to_open_erapi(httpx_mock):
    """Google blocks (e.g. 503), open.er-api.com responds."""
    httpx_mock.add_response(
        url=re.compile(r"https://www\.google\.com/finance/.*"),
        status_code=503,
        text="blocked",
    )
    httpx_mock.add_response(
        url="https://open.er-api.com/v6/latest/AED",
        json={
            "result": "success",
            "rates": {"INR": 25.879247, "USD": 0.272, "EUR": 0.232},
        },
    )
    q = MidMarketProvider().fetch(target_currency="INR")
    assert q.status == "ok"
    assert q.rate == 25.879247
    assert "Open ExchangeRate" in (q.note or "")


# --- Gold (UAE / India current sources) ---------------------------------

def test_uae_gold_extracts_24k_and_22k(httpx_mock):
    httpx_mock.add_response(
        url=re.compile(r"https://charts\.igold\.ae/api/data.*metal=xau.*"),
        json={
            "last_price": 556.00,
            "data": [
                [1777593600000, 540.00],
                [1777680000000, 550.00],
                [1777766400000, 556.00],
            ],
        },
        headers={"content-type": "application/json"},
    )
    side = fetch_uae_gold()
    assert side.status == "ok"
    assert side.currency == "AED"
    assert side.per_g_24k == 556.00
    assert side.per_g_22k == pytest.approx(509.67)
    assert "igold.ae" in side.source
    assert len(side.history) == 3
    assert side.history[-1].date == "2026-05-03"
    assert side.history[-1].per_g_22k == pytest.approx(509.67)


def test_india_gold_extracts_history_series(httpx_mock):
    httpx_mock.add_response(
        url=re.compile(r"https://www\.livechennai\.com/gold_silverrate\.asp.*"),
        text=fixture_text("livechennai_gold_silver.html"),
        headers={"content-type": "text/html"},
    )
    side = fetch_india_gold(history_days=30)
    assert side.status == "ok"
    assert side.currency == "INR"
    assert side.per_g_24k == 14632.0
    assert side.per_g_22k == 13935.0
    assert side.source == "LiveChennai"
    # LiveChennai fixture carries 3 recent daily rows, newest first.
    assert len(side.history) == 3
    assert side.history[0].date == "2026-05-02"
    assert side.history[0].per_g_24k == 14632.0


# --- Sanity: every provider in the registry has a valid id -------------

def test_all_providers_have_id_and_name():
    """Every PROVIDERS entry must have non-empty id and display_name."""
    from scrapers.run_all import PROVIDERS
    for p in PROVIDERS:
        assert p.id, f"{type(p).__name__} has empty id"
        assert p.display_name, f"{type(p).__name__} has empty display_name"
        # Provider id slugs should be lowercase, alphanumeric, with underscores
        assert re.fullmatch(r"[a-z0-9_]+", p.id), f"{p.id} is not a valid slug"
