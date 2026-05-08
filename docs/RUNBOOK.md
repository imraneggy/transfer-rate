# Runbook

Operational playbook for the maintainer. Each entry: symptom → likely cause →
fix → verification.

## Cron scrape stopped running

**Symptom**: `rates.json` `completed_at` is older than 30 minutes; no recent
runs in the Actions tab.

**Likely causes**:
1. Repo went 60 days without activity → GitHub disables scheduled workflows.
2. Workflow YAML invalid after a recent change.
3. Default branch renamed.

**Fix**:
1. Push any commit (even a no-op `docs:` commit) to wake scheduled jobs.
2. Open the workflow in the Actions tab → "Enable workflow."
3. Re-trigger manually with `workflow_dispatch`.

**Verify**: New run appears in Actions list, exits green, `rates.json`
`completed_at` is current.

## A specific provider has been `error` for >2 hours

**Symptom**: `rates.json` shows `status="error"` for one provider across
several runs; others are `ok`.

**Likely causes**:
1. Provider site changed HTML structure → parser regex no longer matches.
2. Provider added bot detection (Cloudflare challenge, captcha).
3. Provider IP-blocked the GitHub Actions egress range.

**Diagnose**:
1. Look at the `note` field in `rates.json` for that provider — it
   contains the exception type and message.
2. Run the scraper locally:
   ```bash
   python -c "from scrapers.lulu import LuluProvider; print(LuluProvider().fetch_inr())"
   ```
3. Save the raw HTML to inspect:
   ```bash
   curl -A "transfer-rate-bot/1.0" https://www.lulumoney.com/exchange-rates > /tmp/lulu.html
   ```

**Fix**:
* Parser drift → update the selector/regex in the provider's `.py` file.
* Bot detection → consider downgrading the provider to `investigating`
  (stub) until a stable source is found.
* IP block → option A: add a public Cloudflare Workers proxy in front of
  the scrapers (small extra moving piece); option B: stub the provider.

**Verify**: Local run shows `ok`; commit + push; Actions run shows the
provider back to `ok`.

## App shows "Couldn't load rates"

**Symptom**: Many users report the error screen.

**Likely causes**:
1. GitHub Pages incident.
2. JSON malformed by a recent scraper change.
3. App version expecting a newer schema_version.

**Diagnose**:
```bash
curl -i https://imraneggy.github.io/transfer-rate/rates.json
```

* HTTP 200 + valid JSON → it's a client-side issue. Check Play Console
  logs / device-side reports.
* HTTP 5xx → GitHub Pages incident; watch
  https://www.githubstatus.com/.
* HTTP 200 + malformed JSON → revert the offending commit:
  ```bash
  git revert <bad_commit>
  git push
  # the pages workflow republishes within ~2 minutes
  ```

**Verify**: `curl` returns valid JSON; relaunch the app — rates load.

## A provider asks for removal

See `TAKEDOWN.md`. Process:

1. Confirm the requester is authorised at the provider.
2. Open a `Takedown` issue documenting the request.
3. Edit `scrapers/run_all.py` to remove the provider from `PROVIDERS`.
4. Optionally delete `scrapers/<provider>.py` (keeps git history).
5. Push to `main`. Next scrape removes the provider from `rates.json`.
6. Reply to the requester confirming removal and link the commit.

Target turnaround: under 24 hours.

## Rotating release-signing key

**When**: every 1–2 years, or immediately if you believe the key has leaked.

**How**:
1. Generate a new keystore:
   ```bash
   keytool -genkey -v -keystore android/app/release.keystore \
     -alias transferrate -keyalg RSA -keysize 4096 -validity 10000
   ```
2. Update environment variables `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
   `KEY_PASSWORD` in your build environment.
3. Upload the new key to Play App Signing only if Play Console asks
   (most uploads use the same upload key indefinitely).
4. **Never** commit a keystore to the repo.

## Emergency kill-switch (publishing)

If you need to stop the data flow immediately (e.g., scraper accidentally
exfiltrating something it shouldn't):

```bash
# Disable both workflows
gh workflow disable scrape.yml
gh workflow disable pages.yml
```

Existing `rates.json` continues to serve from Pages (cached). To also
remove the served file:

```bash
git rm public/rates.json
git commit -m "ops: pull rates.json"
git push
```

## Reliable 15-minute refresh

GitHub Actions throttles `schedule:` workflows on the free tier — our
`cron: 0,15,30,45 * * * *` definition fires every 15 minutes ON PAPER, but
in practice GitHub may delay it to roughly hourly during peak load. The
official documented workaround is to use `workflow_dispatch` (which is
NOT subject to the same throttling) via an external pinger.

### One-time setup with cron-job.org (free, ~5 minutes)

**Step 1 — create a narrowly-scoped PAT just for this**

1. Visit https://github.com/settings/personal-access-tokens/new
2. **Token name**: `transfer-rate-cron-pinger`
3. **Expiration**: 1 year (set a calendar reminder to rotate)
4. **Resource owner**: your account
5. **Repository access**: *Only select repositories* → `imraneggy/transfer-rate`
6. **Permissions** → Repository permissions:
   - **Actions**: *Read and write* (this is the only permission needed)
   - Leave all others as default (No access)
7. Generate token → copy it. You will not see it again.

This token cannot read your code, modify files, or do anything except
trigger your workflows. If cron-job.org is ever breached, the worst
attacker can do is fire your scrape workflow more often.

**Step 2 — set up the cron job**

1. Sign up free at https://cron-job.org/en/signup/. Verify the email.
2. Click **Create cronjob** → fill in:
   - **Title**: `transfer-rate scrape`
   - **URL**:
     ```
     https://api.github.com/repos/imraneggy/transfer-rate/actions/workflows/269638606/dispatches
     ```
   - **Schedule** → **Every 15 minutes**: tick all minutes that are multiples
     of 15 (`0`, `15`, `30`, `45`), all hours, all days.
   - **Save** → not yet, first set the request details below.
3. Switch to the **Advanced** tab:
   - **Request method**: `POST`
   - **Request body** (raw): `{"ref":"main"}`
   - **Headers** — add three:
     ```
     Authorization: Bearer <paste-the-PAT-from-step-1>
     Accept: application/vnd.github+json
     Content-Type: application/json
     ```
4. Save the cronjob.
5. Run it once manually (the **Run** button on the cronjob detail page) to
   verify. Expected result: HTTP 204 No Content.

**Step 3 — verify it works**

Open https://github.com/imraneggy/transfer-rate/actions/workflows/scrape.yml.
You should see a new run firing every 15 minutes from now on, with the
trigger event shown as `workflow_dispatch`.

If you ever need to disable: log into cron-job.org and toggle the cronjob
off. The schedule-based runs will continue at their (slower) cadence.

### Token rotation

The PAT created above expires in 1 year. To rotate:

1. Generate a new one with the same scope (steps 1 above).
2. Edit the cronjob in cron-job.org → Advanced → Headers → replace the
   `Authorization` header with the new token.
3. Save → run once to verify.
4. Delete the old token from your GitHub PAT list.

## Manual rate entry (admin UI)

For app-only providers without a public rate endpoint (Botim, e&amp; Money,
Comera, Careem Pay, NowMoney), or for any other provider you've personally
verified, you can enter rates manually:

1. **Open the admin URL**: <https://imraneggy.github.io/transfer-rate/admin/>
2. **Paste a fine-grained PAT** with `contents: read+write` permission on
   `imraneggy/transfer-rate`. The token persists in your browser's
   localStorage so you only enter it once per device.
3. **Enter rates per (provider, currency)** — leave blanks for fields you
   don't want to set. Inputs are validated against per-currency plausible
   bounds (e.g. INR must be 10–50 per AED).
4. **Click Save** — the admin page commits the new
   `data/manual-rates.json` to the repo via GitHub's Contents API.
5. **Wait for next cron tick (~hourly)** OR manually trigger the scrape
   workflow.  Read the PAT from an environment variable (or the GitHub
   CLI keychain), **never** from a plaintext file in the working
   directory — `git.txt`-style files are easy to forget about and
   accidentally commit.

   ```bash
   # Option A — environment variable (recommended).  Set GH_TOKEN once
   # in your shell profile (~/.zshrc / ~/.bashrc) or paste at use site:
   GH_TOKEN="${GH_TOKEN:?Set GH_TOKEN in your environment}" \
     curl -X POST \
       -H "Authorization: Bearer $GH_TOKEN" \
       -H "Accept: application/vnd.github+json" \
       https://api.github.com/repos/imraneggy/transfer-rate/actions/workflows/269638606/dispatches \
       -d '{"ref":"main"}'

   # Option B — GitHub CLI (manages the token in the OS keychain):
   gh workflow run scrape.yml --ref main
   ```

Manual rates appear in the app with a `MANUAL` badge to distinguish from
auto-scraped values. They override `status="investigating"` stubs only —
they do NOT override working scrapers (which is by design; if a scraper
breaks, you fix it rather than mask it).

### When to clear a manual entry

- Set the input to blank and Save. The next commit removes that
  (provider, currency) entry.
- The corresponding card in the app reverts to the mid-market estimate.

### When to refresh manual entries

- Daily for app-only providers — rates change throughout the day, and
  yesterday's manual entry quickly becomes misleading.
- After a stale entry's `fetched_at` exceeds 24 hours, consider it
  unreliable. The orchestrator does not auto-expire manual entries, so
  this is your responsibility.

## On-call expectation

Best-effort. This is volunteer-operated open source. SLAs are aspirational:

* Response to security report: 72 hours
* Response to takedown request: 24 hours
* Wrong-rate fix: same week
