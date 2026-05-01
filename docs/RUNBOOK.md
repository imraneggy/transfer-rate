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

## On-call expectation

Best-effort. This is volunteer-operated open source. SLAs are aspirational:

* Response to security report: 72 hours
* Response to takedown request: 24 hours
* Wrong-rate fix: same week
