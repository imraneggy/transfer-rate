"""Shared pytest configuration and helpers."""
from __future__ import annotations

from pathlib import Path
from typing import Any

import pytest


FIXTURES = Path(__file__).parent / "fixtures"


def fixture_text(name: str) -> str:
    """Load a captured response fixture as text."""
    return (FIXTURES / name).read_text(encoding="utf-8")


def fixture_json(name: str) -> Any:
    import json
    return json.loads(fixture_text(name))


@pytest.fixture
def fixtures_dir() -> Path:
    return FIXTURES
