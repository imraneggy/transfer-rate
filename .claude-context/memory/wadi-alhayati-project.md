---
name: Wadi Alhayati Tourism site project
description: Wadi Alhayati Travel & Tourism L.L.C. immersive 3D landing site — Next.js 15 + R3F + GSAP + Sanity CMS, lead-capture only via Resend + WhatsApp
type: project
originSessionId: 41920dff-9de7-4559-afc5-e8e27f802bd1
---
**Project:** Wadi Alhayati Travel & Tourism L.L.C. — Frij Al Murar, Deira, Dubai.

Why: Operator needed a flagship landing site for air tickets, hotels, and chauffeured UAE tour packages with "step-inside-the-UAE" 3D vibe; no real booking APIs in v1 (lead-capture only, deferred to v2).

How to apply: When iterating on this project's site, the locked decisions are:
- **Code location:** `C:\Users\imran.batcha\Projects\wadi-alhayati`
- **Stack:** Next.js 15 App Router + React 19 + Tailwind v4 (CSS-first `@theme` tokens in `app/globals.css`) + React Three Fiber v8 + GSAP + Sanity CMS v3 (embedded at `/studio`) + Resend for email leads
- **Design direction:** "Cinematic Desert Sunset" — navy `#0E2A52`, gold-royal `#C9A04A`, sky-day `#3BA9E0`, sand-cream `#F7ECD2`, sunset-orange `#E67E3B`, dune-shadow `#6B4A2B`. Fonts: Cormorant Garamond (display) + Inter (body) + Italianno (script tagline)
- **Booking model v1:** Lead-capture only — `app/api/inquiry/route.ts` returns `wa.me/971503952319` deep-link with pre-filled summary, and uses Resend to email `wadialhayatitourism@gmail.com` when `RESEND_API_KEY` is set (graceful fallback when not)
- **Brand source of truth:** `lib/config/brand.ts` — phones, email, address; edit there and the whole site updates
- **Logo file (raw card):** `C:\Users\imran.batcha\OneDrive - Ali & Sons Holding L.L.C\Documents\Wadi Haya\Brand Logo.jpg` (copied to `public/brand/logo-card.jpg`; the nav uses an SVG `<WMark>` component, not the raster)
- **Plan file:** `C:\Users\imran.batcha\.claude\plans\creative-and-3d-designer-indexed-music.md`

Deferred to v2 (don't suggest unless asked): real flight/hotel inventory APIs (Amadeus / Hotelbeds), Stripe payments, Arabic RTL multi-language.
