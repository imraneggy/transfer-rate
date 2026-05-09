"""APCA contrast + visual swatch validator for the Transfer Rate palette.

Computes APCA Lc (the WCAG-3.0-candidate contrast metric used by the
codebase's design comments) for every text-on-background pair the app
actually renders, and generates swatch PNGs so the developer can
eyeball legibility against the numerics.

Outputs to `docs/color-validation/`:
    * report.md                — APCA scores for every pair, pass/fail against
                                  the threshold appropriate for that text size.
    * light-swatches.png       — visual grid of every light-mode pair.
    * dark-swatches.png        — visual grid of every dark-mode pair.

Re-run after any palette change:
    python tools/validate_color_palette.py

Exit code 0 if all rendered pairs pass; 1 if any pair fails its
threshold (CI-friendly).
"""

from __future__ import annotations

import sys
from pathlib import Path
from typing import Iterable, NamedTuple

from PIL import Image, ImageDraw, ImageFont


REPO = Path(__file__).resolve().parent.parent
OUT_DIR = REPO / "docs" / "color-validation"


# -------------------- Palette (mirror of Theme.kt v0.29.3) --------------------

# Light scheme — v0.29.3
L = {
    "primary":              "#6F73FF",   # indigo L=63 (tightened from L=66)
    "onPrimary":            "#FFFFFF",
    "primaryContainer":     "#CDD9FF",
    "onPrimaryContainer":   "#241776",
    "secondary":            "#635BFF",
    "onSecondary":          "#FFFFFF",
    "secondaryContainer":   "#A3ACFF",
    "onSecondaryContainer": "#100D3B",
    "background":           "#F6F9FC",
    "onBackground":         "#1F2F41",
    "surface":              "#FFFFFF",
    "onSurface":            "#1F2F41",
    "surfaceVariant":       "#E2EAF3",
    "onSurfaceVariant":     "#4A6684",
    "outline":              "#8EB1D2",   # used only as divider stroke; no longer text
    "outlineVariant":       "#C2D7EE",
}

# Dark scheme — v0.29.3
D = {
    "primary":              "#B0BAFF",   # L=85 (lifted from L=80 for primary-as-text legibility)
    "onPrimary":            "#100D3B",   # L=12 (tightened from L=20 for higher polarity)
    "primaryContainer":     "#3929AD",
    "onPrimaryContainer":   "#DCE7FF",
    "secondary":            "#8486FF",
    "onSecondary":          "#100D3B",
    "secondaryContainer":   "#241776",
    "onSecondaryContainer": "#DCE7FF",
    "background":           "#0F1720",
    "onBackground":         "#E0F1FF",
    "surface":              "#1F2F41",
    "onSurface":            "#E0F1FF",
    "surfaceVariant":       "#334462",
    "onSurfaceVariant":     "#BFD2E5",   # L=85 (lifted from L=80)
    "outline":              "#4A6684",
    "outlineVariant":       "#334462",
}


# -------------------- APCA contrast (Lc) ---------------------------------

def hex_to_rgb(hex_str: str) -> tuple[int, int, int]:
    s = hex_str.lstrip("#")
    return int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16)


def luminance(rgb: tuple[int, int, int]) -> float:
    """sRGB to APCA Y (linear-ish luminance)."""
    r, g, b = rgb
    def lin(c: int) -> float:
        return (c / 255.0) ** 2.4
    return 0.2126 * lin(r) + 0.7152 * lin(g) + 0.0722 * lin(b)


def apca_lc(text_hex: str, bg_hex: str) -> float:
    """APCA Lc value (signed; |Lc| meaningful for thresholds).

    Implementation based on the APCA W3 community draft.  Soft black
    threshold + polarity-aware exponents reflect that the human eye
    perceives dark-text-on-light differently from light-text-on-dark.
    """
    Y_txt = luminance(hex_to_rgb(text_hex))
    Y_bg = luminance(hex_to_rgb(bg_hex))

    BLACK_THRESHOLD = 0.022
    BLACK_CLAMP = 1.414
    if Y_txt < BLACK_THRESHOLD:
        Y_txt = Y_txt + (BLACK_THRESHOLD - Y_txt) ** BLACK_CLAMP
    if Y_bg < BLACK_THRESHOLD:
        Y_bg = Y_bg + (BLACK_THRESHOLD - Y_bg) ** BLACK_CLAMP

    if Y_bg > Y_txt:
        SAPC = (Y_bg ** 0.56 - Y_txt ** 0.57) * 1.14
    else:
        SAPC = (Y_bg ** 0.65 - Y_txt ** 0.62) * 1.14

    if abs(SAPC) < 0.1:
        return 0.0
    Lc = SAPC * 100
    return Lc - 2.7 if Lc > 0 else Lc + 2.7


# Pairs the app actually renders.  Each entry: (purpose, text-key, bg-key,
# size-class).  size-class drives the APCA threshold:
#   "headline"  -> |Lc| >= 45
#   "body"      -> |Lc| >= 60
#   "small"     -> |Lc| >= 75
class Pair(NamedTuple):
    label: str
    text_key: str
    bg_key: str
    size_class: str


PAIRS: list[Pair] = [
    # Body text on app surfaces
    Pair("Body text on bg",                "onSurface",            "background",         "body"),
    Pair("Body text on card surface",      "onSurface",            "surface",            "body"),
    Pair("Body text on card variant",      "onSurface",            "surfaceVariant",     "body"),
    Pair("Secondary text on bg",           "onSurfaceVariant",     "background",         "body"),
    Pair("Secondary text on card",         "onSurfaceVariant",     "surface",            "body"),
    Pair("Secondary text on card variant", "onSurfaceVariant",     "surfaceVariant",     "body"),
    # Buttons / chips — chip labels in this app are 14sp Bold (labelLarge)
    # and 12sp Bold (labelMedium); both are body-class per APCA when bold.
    Pair("Button label on primary",        "onPrimary",            "primary",            "body"),
    Pair("Chip label on primary container",        "onPrimaryContainer",   "primaryContainer",   "body"),
    Pair("Selected chip on primary",       "onPrimary",            "primary",            "body"),
    Pair("Secondary chip on its container",        "onSecondaryContainer", "secondaryContainer", "body"),
    # Brand accents — primary used as text only on neutral surface in
    # tightly-controlled spots (eyebrow labels, AED Set button).  We
    # validate against body threshold for these.
    Pair("Primary-coloured link on surface",       "primary",              "surface",            "body"),
    Pair("Primary-coloured link on bg",            "primary",              "background",         "body"),
    # The previous "Primary-coloured link on card variant" pair was
    # removed in v0.29.3 — the View-full-history button switched from
    # surfaceVariant bg + primary text to a filled primary button, so
    # this combo is no longer rendered anywhere.
    # outline-as-text was eliminated in v0.29.3; outline is now strictly
    # a divider/stroke colour, not a text role.
]


def threshold(size_class: str) -> int:
    return {"headline": 45, "body": 60, "small": 75}[size_class]


# -------------------- Swatch rendering ----------------------------------

def render_swatches(scheme: dict, scheme_name: str, out_path: Path) -> None:
    """Generate a single PNG showing every pair as a swatch.

    Each swatch is a card with the pair's label + a sample of the actual
    text style at typical app size, rendered using the actual colours.
    Output uses the system default font (Pillow's).
    """
    rows = len(PAIRS)
    swatch_w = 720
    swatch_h = 90
    pad = 20
    img_w = swatch_w + pad * 2
    img_h = swatch_h * rows + pad * (rows + 1)

    # Backdrop matches the scheme's background so the swatches read in
    # the context they'd render in-app.
    page_bg = scheme["background"]
    img = Image.new("RGB", (img_w, img_h), color=page_bg)
    draw = ImageDraw.Draw(img)

    try:
        title_font = ImageFont.truetype("arial.ttf", 14)
        body_font = ImageFont.truetype("arial.ttf", 18)
        score_font = ImageFont.truetype("arial.ttf", 12)
    except (OSError, IOError):
        title_font = ImageFont.load_default()
        body_font = ImageFont.load_default()
        score_font = ImageFont.load_default()

    for i, pair in enumerate(PAIRS):
        y = pad + i * (swatch_h + pad)
        text_hex = scheme[pair.text_key]
        bg_hex = scheme[pair.bg_key]
        Lc = apca_lc(text_hex, bg_hex)
        thresh = threshold(pair.size_class)
        passing = abs(Lc) >= thresh
        status = "PASS" if passing else "FAIL"

        # Card body — drawn in the actual bg colour
        draw.rectangle([(pad, y), (pad + swatch_w, y + swatch_h)], fill=bg_hex)

        # Pair label and score above the rendered sample
        draw.text(
            (pad + 16, y + 8),
            f"{pair.label}    [{status}  Lc={Lc:+.0f}  threshold={thresh}]",
            fill=text_hex,
            font=title_font,
        )
        # The actual text rendered in the actual colour, at a body size
        sample = "Sample text — 1 AED -> 25.7800 INR  -  BEST"
        draw.text((pad + 16, y + 36), sample, fill=text_hex, font=body_font)
        # Hex codes in fine print
        draw.text(
            (pad + 16, y + 66),
            f"text {text_hex}  on  bg {bg_hex}",
            fill=text_hex,
            font=score_font,
        )

    out_path.parent.mkdir(parents=True, exist_ok=True)
    img.save(out_path, format="PNG", optimize=True)


def render_report(out_path: Path) -> int:
    """Write a Markdown report and return number of failing pairs."""
    fails = 0
    lines: list[str] = []
    lines.append("# Colour palette validation\n")
    lines.append(
        "Programmatic APCA contrast scoring + visual swatches for "
        "every text-on-background pair the Transfer Rate app renders. "
        "Generated by `tools/validate_color_palette.py`.\n"
    )
    lines.append(
        "**APCA thresholds**\n\n"
        "| Size class       | min |Lc| |\n"
        "|------------------|---------|\n"
        "| Headline (24sp+) | 45      |\n"
        "| Body (14-18sp)   | 60      |\n"
        "| Small (10-13sp)  | 75      |\n\n"
    )

    for scheme_name, scheme in (("Light", L), ("Dark", D)):
        lines.append(f"## {scheme_name} scheme\n")
        lines.append(
            "| Purpose | text | bg | Lc | threshold | result |\n"
            "|---------|------|----|----|-----------|--------|\n"
        )
        for pair in PAIRS:
            text_hex = scheme[pair.text_key]
            bg_hex = scheme[pair.bg_key]
            Lc = apca_lc(text_hex, bg_hex)
            thresh = threshold(pair.size_class)
            passing = abs(Lc) >= thresh
            if not passing:
                fails += 1
            status = "PASS" if passing else "**FAIL**"
            lines.append(
                f"| {pair.label} "
                f"| `{text_hex}` ({pair.text_key}) "
                f"| `{bg_hex}` ({pair.bg_key}) "
                f"| {Lc:+.0f} "
                f"| {thresh} "
                f"| {status} |\n"
            )
        # link to the swatch image
        scheme_lower = scheme_name.lower()
        lines.append(
            f"\n![{scheme_name} swatches]({scheme_lower}-swatches.png)\n\n"
        )

    out_path.write_text("".join(lines), encoding="utf-8")
    return fails


def main() -> int:
    print(f"Output dir: {OUT_DIR}")
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    print("Rendering light-mode swatches...")
    render_swatches(L, "Light", OUT_DIR / "light-swatches.png")
    print("Rendering dark-mode swatches...")
    render_swatches(D, "Dark", OUT_DIR / "dark-swatches.png")
    print("Writing markdown report...")
    fails = render_report(OUT_DIR / "report.md")

    print()
    print("---")
    print(f"Wrote {OUT_DIR / 'report.md'}")
    print(f"Wrote {OUT_DIR / 'light-swatches.png'}")
    print(f"Wrote {OUT_DIR / 'dark-swatches.png'}")
    print()
    if fails:
        print(f"FAILURE: {fails} pairs do not meet their APCA threshold.")
        print("Open the report.md and the *-swatches.png to see which.")
        return 1
    print("All rendered pairs pass APCA threshold for their text size.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
