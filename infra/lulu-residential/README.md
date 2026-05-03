# Self-hosted runner for LuLu Exchange

LuLu's rate API runs on TCP port 9443. GitHub-hosted runners block this
port. Cloudflare Workers also can't reach non-standard ports. The only
practical fix is to run the LuLu fetch from a machine with residential-
IP egress.

This guide walks you through installing a single GitHub Actions
self-hosted runner on whatever box you choose. Total time: **~5
minutes**. Free if you reuse hardware you already own.

## Recommended hardware

Anything that can run Python 3.12 and stay online ~95% of the time:

| Option | Cost | Notes |
|---|---|---|
| Your home laptop / desktop | $0 | Easiest. Runner only fires every 30 min so battery impact is negligible. |
| Old Raspberry Pi 4 | $0-50 | Idle power ~3W; perfect for an always-on box. |
| $5/mo Indian VPS with residential ISP | ~$60/yr | Best uptime. DigitalOcean, Linode, etc. — but verify the provider isn't an ASN that LuLu's CDN classifies as datacenter. Search "residential VPS India" or test the IP at https://www.whoisxmlapi.com/. |

If you don't have a box ready, the cheapest get-started path is your own
laptop — leave it plugged in overnight, you'll wake up with hourly LuLu
data.

## Setup (5 steps)

### 1. Create the runner registration token

Open: **https://github.com/imraneggy/transfer-rate/settings/actions/runners/new?arch=x64&os=linux**

(Change `os=` to `windows` or `osx` if your box is Windows / Mac.)

GitHub shows you a one-line registration token (`ABCD...`). Don't close
this page — you'll copy commands from it next.

### 2. Install the runner on your box

GitHub's page above shows the exact 4-line install for your OS. Run
those commands on your box (SSH in if it's a VPS, or open a terminal if
it's local). Example for Linux:

```bash
mkdir actions-runner && cd actions-runner
curl -o actions-runner-linux-x64.tar.gz -L https://github.com/actions/runner/releases/download/...
tar xzf actions-runner-linux-x64.tar.gz
./config.sh --url https://github.com/imraneggy/transfer-rate --token <YOUR_TOKEN>
```

When `config.sh` asks:
- **Runner name** → press Enter for default
- **Runner labels** → type **`lulu-residential`** (this label is what
  the workflow targets — must match exactly)
- **Work folder** → press Enter for default

### 3. Start the runner as a service

Linux:
```bash
sudo ./svc.sh install
sudo ./svc.sh start
```

macOS:
```bash
./svc.sh install
./svc.sh start
```

Windows: from an elevated PowerShell:
```powershell
.\svc.sh install
.\svc.sh start
```

The runner is now listening for jobs in the background.

### 4. Verify it shows up online

Open: **https://github.com/imraneggy/transfer-rate/settings/actions/runners**

You should see your runner with a green "Idle" badge and the
`lulu-residential` label.

### 5. Trigger the workflow

Open: **https://github.com/imraneggy/transfer-rate/actions/workflows/scrape-lulu-residential.yml**
→ **Run workflow** → **Run workflow** (green button).

Within ~30 seconds, the run will:

1. Pick up the job on your runner
2. Fetch LuLu's rate via the working port-9443 endpoint
3. Inject it into `public/rates.json`
4. Push the change

Check the live JSON afterwards — LuLu should appear at index 5 of
`corridors.INR`:

```
https://imraneggy.github.io/transfer-rate/rates.json
```

## What happens after that

The workflow runs every 30 minutes via cron. Your box does ~2 minutes of
work twice an hour. Your home laptop won't notice; a Pi will sit at
near-zero idle.

If you take the box offline (vacation, restart, etc.) the workflow
queues. Once the runner reconnects, it catches up — the JSON simply
keeps the previous LuLu entry as `status: ok` until the next run
overwrites it.

## Removing the runner

If you ever want to stop:

1. **Stop the service**:
   ```bash
   sudo ./svc.sh stop && sudo ./svc.sh uninstall
   ```
2. **Remove from GitHub**: Settings → Actions → Runners → click your
   runner → **Remove runner** → **Remove**.
3. The `scrape-lulu-residential.yml` workflow runs will queue forever
   (harmless) — to clean up, delete the workflow file:
   `rm .github/workflows/scrape-lulu-residential.yml && git push`.

LuLu's rate will go stale within ~30 min of the last successful run,
then disappear from the JSON the next time the main scrape rebuilds it.

## Cost

Zero, unless you rented a VPS. The GitHub Actions self-hosted-runner
infrastructure is free for any number of runners on public repos.
