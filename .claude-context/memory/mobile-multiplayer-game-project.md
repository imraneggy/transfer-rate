---
name: Project Arasan — Tamil mobile open-world story game
description: Mobile-first hybrid 3D+2D GTA-style adventure; Tamil-targeted; Godot 4.6 landscape mobile; fictional Madhuranadu/Madurapuri setting
type: project
originSessionId: c87e6a1e-0721-4a99-9adb-2737836c22d0
---
**Repo:** `imraneggy/mobile-multiplayer-game` (private, MIT for code). Local path: `C:\Users\imran.batcha\Projects\mobile-multiplayer-game`. Latest commit `3144880` (2026-05-15) — mobile-first pivot.

**Working title:** *Project Arasan*

**Two pivots already (history):**
1. (2026-05-14) Original framing → 2D narrative PC game (Disco-Elysium-style). Locked design + made Godot 2D skeleton.
2. (2026-05-15) User changed direction → mobile-first hybrid 3D+2D GTA-SA-style. All docs + Godot skeleton rewritten.

**Current direction (locked 2026-05-15):**
- **Platform**: Mobile-first, Android (Play Store) before iOS/Steam
- **Orientation**: Landscape
- **Gameplay**: Hybrid — 3D stylized low-poly open-world exploration + missions, 2D dialogue scenes for narrative depth
- **Art style**: Stylized low-poly (PS2 / GTA San Andreas aesthetic). Renders 60fps on mid-range Snapdragon 7 Gen 2; 30fps on Snapdragon 6 Gen 1
- **Audience**: Tamil diaspora globally (~80M) + Tamil-speaking India (~50M). UI Tamil + English; voice acting Tamil
- **Monetization**: F2P with chapter packs. Chapter 1 FREE; chapters 2–6 IAP at $2.99 each; season pass $9.99
- **Engine**: Godot 4.6 Standard, GDScript, GL Compatibility renderer (OpenGL ES 3.0)
- **Dialogue**: Dialogic 3 addon
- **Distribution**: Google Play Store first, $25 dev fee one-time

**Story (unchanged across both pivots):**
- Protagonist: **Arasan Periyasamy** ("Arasan" = king in Tamil), born in slum
- World: fictional state **Madhuranadu**, capital **Madurapuri**
- Three acts × six chapters: Slum tea-stall boy → cinema mega-star → Chief Minister
- Inspiration: MGR/Jayalalithaa/Vijayakanth/Kamal Haasan/Vijay arc (Vijay is sitting TN CM as of 2026-05-10 — strict fictionalisation required, see `docs/legal-safety.md`)
- 4 player attributes: Mass Appeal, Political Savvy, Conviction, Resources
- Supporting cast: R. Mannar Velu (mentor), Bharathi Krishnaveni (journalist + love interest), Selvanathan III (TKK adversary), Periyammal (mother), Director Kavi, Bhargav Reddy (industrialist), Constable Mani, Anbu (best friend)
- Fictional parties: TKK (Tamizhar Kalvi Kazhagam), MVK (Makkal Vetri Kazhagam), MAK (Makkal Arasiyal Kazhagam — Arasan's party)

**Vertical slice = Chapter 1 (6 months)**
- One 3D neighborhood: Senthamizh Kuppam slum (~150m×150m)
- 1 vehicle: auto-rickshaw
- 6 hero character models (Arasan, Periyammal, Anbu, Velumani, Mani, Mannar)
- 3 missions: Help Mother / Race Anbu auto-rickshaw / Perform at temple festival
- 5 voiced Tamil dialogue scenes (~12 min audio total)
- ~30–45 min playtime
- Free on Play Store as demo

**Repo state (commit 3144880):**
- All 5 core docs rewritten for mobile-first (README, GDD, tech-stack, architecture, monetization)
- NEW: `docs/storyboard-chapter-1.md` — full scene-by-scene Chapter 1 storyboard
- `docs/legal-safety.md` — unchanged (fictionalisation rules), still applies
- Godot project skeleton: landscape orientation, `scenes/ui/main_menu.tscn` redesigned for landscape touch, placeholder `scenes/world/senthamizh_kuppam.tscn` with sky+ground+light
- GameState v2 with mission/IAP/in-game-clock fields + v1→v2 migration

**User explicit instruction (2026-05-15):** "constantly keep updating the changes we make in github, full story board and full documentations without fail" — *commit + push after every meaningful change going forward*.

**Next steps (Phase 1, ~4 weeks of evenings):**
1. User to install Godot 4.6 Standard + Android export template + Android Studio
2. Implement third-person mobile character controller (virtual joystick + camera follow)
3. Build first walkable scene: drop Arasan capsule into Senthamizh Kuppam placeholder, walk around
4. Install Dialogic 3 addon
5. Build first Mode 3D ↔ Mode 2D transition (talk to mother trigger)

**GitHub token:** `C:\Users\imran.batcha\OneDrive - Ali & Sons Holding L.L.C\Documents\Files\ai-jbot\git.txt` (fine-grained PAT, expires 2026-06-02). Account `imraneggy`, user ID 219785980. Commit email: `219785980+imraneggy@users.noreply.github.com`.
