# LuLu Exchange rate-proxy Cloudflare Worker

A tiny Cloudflare Worker that proxies the LuLu Exchange live-rate
endpoint (`https://lieservices.luluone.com:9443/liveccyrates`) over
standard port 443 so GitHub Actions runners — which firewall outbound
traffic to non-standard ports — can reach it.

Deploys in under five minutes on Cloudflare's free tier.

## What this enables

LuLu Exchange has been the only consistently-blocked provider in the
Exchangia rate aggregator. Their rate API runs on TCP port 9443 and the
GitHub Actions runner network blocks egress to non-standard ports. The
scraper works fine on residential IPs but cannot run unattended.

This Worker accepts the same query-string payload the LuLu scraper
already builds, forwards it (only when it matches the documented
single-corridor signature), and returns LuLu's JSON unchanged. Your
scraper then points at the Worker URL instead of the upstream.

## What this is not

- It is **not an open proxy.** Only the documented payload shape is
  forwarded; everything else returns 403.
- It does **not store, log, or modify** any of the rate data.
- It does **not require sensitive credentials.** The Gravitee API key
  used upstream is also embedded in LuLu's public homepage JavaScript;
  every browser visitor sends it.

## Deploy in five steps

1. **Create a Cloudflare account** if you don't have one — free tier
   is more than sufficient (100 000 requests/day; this Worker uses
   ~720/month for the hourly cron).

2. **Open the Workers dashboard:**
   `https://dash.cloudflare.com/?to=/:account/workers-and-pages`

3. **Create → Worker → Hello World** template, then click *Edit code*.
   Replace the entire contents with [`worker.js`](./worker.js) from
   this directory.

4. **Click *Deploy*** and copy the assigned URL — it will look like
   `https://lulu-proxy.<your-subdomain>.workers.dev`.

5. **Add the URL as a repository secret:** Settings → Secrets and
   variables → Actions → New repository secret →
   - Name: `LULU_PROXY_URL`
   - Value: the full URL from step 4 (no trailing slash)

The next scheduled scrape (or one triggered manually) will pick up the
secret and start including LuLu in the rates list.

## Verifying it works

After deploying, hit the worker URL with the LuLu payload to verify
end-to-end:

```bash
curl 'https://lulu-proxy.<subdomain>.workers.dev/?payload=%7B%22activityType%22%3A%22rates.get%22%2C%22aglcid%22%3A784278%2C%22instype%22%3A%22LR%22%7D'
```

You should get back the same JSON shape as `lieservices.luluone.com:9443/liveccyrates`:

```json
{"code":...,"message":...,"payload":{"rates":[
  {"frmccy":"AED","toccy":"INR","rate":25.7300,"sellrate":...,"buyrate":...},
  ...
]}}
```

## Rolling back

If you ever want to stop using the worker:

1. Delete the `LULU_PROXY_URL` repository secret.
2. The scraper falls back to its original direct-port-9443 path
   (which will fail in CI, returning `status: error` for LuLu — same
   state we had before).
3. Optional: delete the Worker in the Cloudflare dashboard.

## Cost

Free tier. Cloudflare Workers free plan: 100 000 requests/day.
Exchangia hits the proxy at most once per scheduled scrape (currently
every ~hour, ≈720/month). You will not pay anything.
