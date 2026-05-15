---
name: Design iteration preferences
description: How the user wants design changes proposed and confirmed before code is written
type: feedback
originSessionId: 76d9bf25-7e54-4c08-a667-1570a1f76389
---
**Confirm visual direction (palettes, layouts, fonts) with the user *before* writing code.** Show 2–3 distinct options with concrete hex values + a one-line rationale per option, let the user pick a letter, then implement.

**Why:** repeated rejections of speculative implementations across the v0.26 design pass — first three "safe" directions, then three "modern" directions, and finally three "color-science" directions — only the last set was approved (G — Stripe Atlas Premium). Writing code before alignment burns iterations.

**How to apply:** when the user reports a visual problem ("too dark", "feels off"), respond with options + ask for a pick before coding. When the user says "ship" or picks a letter, implement immediately without re-asking.

**Adjacent rule — primary colors:** the user found Stripe indigo `#635BFF` "too dark" at L=60. Treat ~L=58 as a working floor for a light-mode primary on this app; lean toward L=65–72. Lighter / brighter primaries fit the brand better than deep ones.
