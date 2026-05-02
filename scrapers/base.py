from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass, asdict
from datetime import datetime, timezone
from typing import Optional


# Active corridors the orchestrator generates data for.
#
# Currently AED -> INR only. The architecture supports multi-currency
# (every scraper, the schema, the UI all handle a list of targets) — to
# re-enable a corridor, just add its 3-letter code back to this tuple.
# Available codes for re-enabling: PKR, PHP, BDT, EGP, USD, EUR, GBP, NPR, LKR.
SUPPORTED_TARGETS = (
    "INR",
)


@dataclass
class Quote:
    """A single rate quote from one provider for one (base, quote) pair.

    Any field a scraper cannot reliably extract is left as None. Partial
    data is more useful than no data, and the app treats None as "not shown."
    """
    provider_id: str
    provider_name: str
    base: str                      # "AED"
    quote: str                     # "INR" / "USD" / etc.
    amount_base: float
    rate: Optional[float]
    fee_base: Optional[float]
    received_quote: Optional[float]
    effective_rate: Optional[float]
    delivery_estimate: Optional[str]
    url: Optional[str]
    status: str = "ok"             # "ok" | "stale" | "error" | "investigating"
    note: Optional[str] = None

    # Optional first-transfer / promotional rate. Null when not applicable.
    promo_rate: Optional[float] = None
    promo_note: Optional[str] = None

    fetched_at: str = ""

    def to_dict(self) -> dict:
        return asdict(self)


class BaseProvider(ABC):
    """Abstract scraper interface. Concrete subclasses implement `fetch`.

    Each provider may support a subset of the SUPPORTED_TARGETS. Calling
    fetch() with an unsupported target should raise — the orchestrator
    catches and converts to status='error'.
    """
    id: str = ""
    display_name: str = ""

    @abstractmethod
    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        raise NotImplementedError

    def _now_iso(self) -> str:
        return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

    def _stub(
        self,
        note: str,
        target_currency: str = "INR",
        amount_base: float = 1000.0,
        status: str = "investigating",
    ) -> Quote:
        """Helper for providers we haven't reverse-engineered yet."""
        return Quote(
            provider_id=self.id,
            provider_name=self.display_name,
            base="AED",
            quote=target_currency,
            amount_base=amount_base,
            rate=None,
            fee_base=None,
            received_quote=None,
            effective_rate=None,
            delivery_estimate=None,
            url=None,
            status=status,
            note=note,
            fetched_at=self._now_iso(),
        )
