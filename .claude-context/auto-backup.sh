#!/bin/bash
# .claude-context/auto-backup.sh
#
# Continuous backup of Claude Code state to the transfer-rate git repo.
# Wired as a Stop hook in ~/.claude/settings.json so it runs after every
# conversation turn.  Designed for the workstation-handover scenario:
# user might lose the machine at any moment, so the latest memory +
# conversation transcript stays mirrored to git in near-real-time.
#
# Safety: never modifies the user's branch state mid-edit.  If there are
# uncommitted changes OUTSIDE .claude-context/ (i.e. user / Claude is
# actively working on something), the backup defers — waits for a
# quieter moment.  In practice the window between commits is wide enough
# that the backup fires several times per session.
#
# Failures are silent: if git push fails (no network, rate-limited,
# auth expired), the hook just logs to /tmp/claude-backup.log and
# exits 0 so Claude Code doesn't see an error.

set +e   # best-effort throughout — never block the user
exec 2>>"/tmp/claude-backup.log"

REPO="/c/Users/imran.batcha/AppData/Local/Temp/transfer-rate"
MEMORY_DIR="/c/Users/imran.batcha/.claude/projects/C--Users-imran-batcha/memory"
SESSION_DIR="/c/Users/imran.batcha/.claude/projects/C--Users-imran-batcha"

# Bail if the repo doesn't exist on this machine (post-handover scenario)
[ -d "$REPO/.git" ] || exit 0

cd "$REPO" || exit 0

# 1. Refresh backup files in .claude-context/
mkdir -p .claude-context/memory .claude-context/sessions

# Copy memory .md files (small, fast)
if [ -d "$MEMORY_DIR" ]; then
    cp -f "$MEMORY_DIR"/*.md .claude-context/memory/ 2>/dev/null
fi

# Gzip the most-recently-modified session JSONL
if [ -d "$SESSION_DIR" ]; then
    LATEST_JSONL=$(ls -t "$SESSION_DIR"/*.jsonl 2>/dev/null | head -1)
    if [ -n "$LATEST_JSONL" ] && [ -f "$LATEST_JSONL" ]; then
        BASENAME=$(basename "$LATEST_JSONL")
        gzip -c "$LATEST_JSONL" > ".claude-context/sessions/${BASENAME}.gz"
    fi
fi

# 2. Decide whether to commit + push.
# Only commit if working tree is clean outside .claude-context/ — we
# don't want to mix backup commits with user's WIP feature commits.
DIRTY_OTHER=$(git status --porcelain | grep -v "^[ ?M][ ?M] \.claude-context/" | grep -v "^[ ?M][ ?M]\.claude-context/")

if [ -n "$DIRTY_OTHER" ]; then
    echo "[$(date -Iseconds)] skipping: user has WIP outside .claude-context/" >> /tmp/claude-backup.log
    exit 0
fi

# Anything actually new in .claude-context/?
if [ -z "$(git status --porcelain .claude-context/ 2>/dev/null)" ]; then
    # No changes — nothing to commit
    exit 0
fi

# 3. Stage + commit + push
git add .claude-context/ 2>/dev/null

# Skip CI on these commits so they don't burn android-build minutes
NOW=$(date -u +%Y-%m-%dT%H:%M:%SZ)
git -c user.email="imraneggy@users.noreply.github.com" \
    -c user.name="Imran (auto-backup)" \
    commit -m "chore(claude-backup): auto-sync ${NOW} [skip ci]" \
    --quiet 2>/dev/null

# Push to origin/main quietly.  If this fails (rate limit, network),
# the local commit stays — next turn's hook will pick up and retry.
git push origin main --quiet 2>/dev/null

echo "[${NOW}] backup committed + pushed" >> /tmp/claude-backup.log
exit 0
