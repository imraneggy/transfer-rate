---
name: Wadi Alhayati — auto-push every change to GitHub
description: After every code change on the Wadi Alhayati project, commit and push to GitHub so the auto-deploy ships it immediately
type: feedback
originSessionId: 41920dff-9de7-4559-afc5-e8e27f802bd1
---
For the Wadi Alhayati Tourism project (`C:\Users\imran.batcha\Projects\wadi-alhayati` → `github.com/imraneggy/wadi-alhayati`): after every batch of code changes, **immediately commit and push** so the GitHub Actions → Vercel pipeline ships the new version. The user wants the live site to always reflect the latest local changes — no "I'll commit later" beats.

**Why:** The user is shipping iteratively and reviews changes on the live `https://wadi-alhayati.vercel.app` URL on phone + web. Local-only changes leave them stuck testing nothing — they explicitly asked for "all changes should be simultaneously updated in github".

**How to apply:**
- After finishing a logical batch of file edits, run `git add -A && git commit -m "..." && git push origin main` from the project root before reporting completion.
- Use the GitHub no-reply email for the author: `219785980+imraneggy@users.noreply.github.com` (already set in this repo's git config) — otherwise Vercel blocks the deploy.
- Push using the extraheader auth trick (token read from `C:\Users\imran.batcha\OneDrive - Ali & Sons Holding L.L.C\Documents\Files\ai-jbot\git.txt`) — Git Credential Manager hangs without it on this machine.
- Don't push for one-off exploratory edits the user explicitly says are tentative.
