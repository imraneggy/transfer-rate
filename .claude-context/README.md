# Claude Code Context Backup

Snapshot of the Claude Code state needed to continue Transfer Rate (and
two adjacent projects) on a fresh machine. Committed 2026-05-15 ahead
of a workstation handover.

## Quick map

```
.claude-context/
├── README.md                  ← you are here
├── CONTINUATION_GUIDE.md      ← step-by-step setup on the new machine
├── memory/                    ← Claude Code memory files (small markdown)
│   ├── MEMORY.md              ← the index Claude reads on every turn
│   ├── transfer-rate-project.md
│   ├── wadi-alhayati-project.md
│   ├── mobile-multiplayer-game-project.md
│   ├── feedback-*.md          ← cross-project preferences
│   └── ...
└── sessions/                  ← gzip'd JSONL conversation transcripts
    ├── 76d9bf25-...jsonl.gz   ← May 8 → May 14 (v0.30.x development)
    └── 41920dff-...jsonl.gz   ← May 14 → May 15 (today's v0.31 + v0.32 work)
```

## What's NOT in here (and where to find it on the old machine)

| Item | Old-machine location | Why excluded |
|---|---|---|
| GitHub credentials | Windows Credential Manager | Sensitive — re-auth on new machine via `git push` (prompts for PAT) or `gh auth login` |
| Android keystore | None — CI uses repo secrets | Already in GitHub Actions secrets, no client-side material |
| Cloudflare Worker token | Cloudflare dashboard (cloud-side only) | Not on the old machine in any form |
| Claude Code settings | `~/.claude/settings.json` (if customised) | Not currently customised, defaults fine |

## To restore on a new machine

See `CONTINUATION_GUIDE.md`. TL;DR:

```bash
git clone https://github.com/imraneggy/transfer-rate.git
cp -r transfer-rate/.claude-context/memory/* ~/.claude/projects/<project-id>/memory/
```

Then run `claude` in the cloned repo. The memory files seed the same
project context, so the new session should pick up roughly where the
old one left off.
