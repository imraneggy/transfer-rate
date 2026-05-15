---
name: Use GitHub no-reply email for commits on Vercel-deployed repos
description: Vercel blocks deployments when the commit-author email isn't linked to a GitHub account in the project's scope; use the user's GitHub no-reply email instead of their work email
type: feedback
originSessionId: 41920dff-9de7-4559-afc5-e8e27f802bd1
---
When committing on the user's behalf to a repo that auto-deploys via Vercel, set `git config user.email` to the **GitHub no-reply email** for the relevant account, not their work email.

Format: `{user_id}+{username}@users.noreply.github.com`
Examples seen:
- `imraneggy` (id 219785980) → `219785980+imraneggy@users.noreply.github.com`

**Why:** Vercel enforces "commit email must match a GitHub account in the deployment scope". A work email like `itsecurity@ali-sons.com` that isn't added to the GitHub account triggers "Deployment Blocked — the commit email … could not be matched to a GitHub account". Caused a deploy block on the wadi-alhayati project; required rewriting history with `git filter-branch` + `git push --force-with-lease`.

**How to apply:** Before the first commit on a new repo destined for Vercel, run:
```bash
git config user.name "{github_username}"
git config user.email "{github_id}+{github_username}@users.noreply.github.com"
```
Fetch the user id with: `curl -H "Authorization: Bearer $TOKEN" https://api.github.com/user | jq .id`. If commits already used the wrong email, rewrite with `git filter-branch --env-filter` and force-push (safe on a personal solo branch).
