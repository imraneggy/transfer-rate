# LuLu Exchange rate-proxy Cloudflare Worker

Deploys in 60 seconds. Free forever (Cloudflare's free tier covers
~720 calls/month against a 100 000/day limit).

## Why this is needed

LuLu Exchange's rate API runs on TCP port 9443
(`https://lieservices.luluone.com:9443/liveccyrates`). GitHub
Actions' datacenter runners block all outbound traffic to non-
standard ports, so every direct call from CI times out — even
through a headless browser, because the in-page JavaScript still
hits the same blocked endpoint.

This Worker accepts the documented LuLu payload, forwards it to
upstream:9443 from Cloudflare's network (which has no such block),
and returns the JSON response unchanged. Your `LuluProvider`
scraper then points at the Worker URL instead of the upstream.

## 60-second deploy

1. **Sign in to Cloudflare**: https://dash.cloudflare.com/sign-up
   (free account, no credit card required).

2. **Open Workers & Pages** in the left sidebar →
   **Create** → **Create Worker** → name it anything (e.g.
   `lulu-proxy`) → **Deploy**.

3. **Edit code** → paste the entire contents of
   [`worker.js`](./worker.js) → **Save and Deploy**.

4. **Copy the worker URL** from the top of the page —
   it looks like `https://lulu-proxy.<your-subdomain>.workers.dev`.

5. **Add it as a repository secret** in GitHub:
   - Repo → **Settings** → **Secrets and variables** → **Actions**
   - **New repository secret**:
     - Name: `LULU_PROXY_URL`
     - Value: the URL from step 4 (no trailing slash)

6. **Trigger a fresh scrape** to confirm:
   - Repo → **Actions** → **scrape** → **Run workflow**
   - Within ~90 s, LuLu appears in
     https://imraneggy.github.io/transfer-rate/rates.json

## Verify it works (optional)

```bash
curl 'https://lulu-proxy.<subdomain>.workers.dev/?payload=%7B%22activityType%22%3A%22rates.get%22%2C%22aglcid%22%3A784278%2C%22instype%22%3A%22LR%22%7D'
```

Expected: a JSON envelope with the AED rate set:

```json
{"code":..., "payload":{"rates":[
  {"frmccy":"AED","toccy":"INR","rate":25.7300,"sellrate":...,"buyrate":...},
  ...
]}}
```

## Security stance

This is intentionally **not** an open proxy. The Worker only
forwards requests matching the documented LuLu payload signature
(`activityType: "rates.get"`, fixed UAE `aglcid`, allowed
`instype` values). Anything else returns 403.

The Gravitee API key embedded in the Worker is the same key sent
by every browser visitor of luluexchange.com — it's public by
design. No private credentials are stored.

## Removing the proxy later

1. Delete the `LULU_PROXY_URL` repo secret.
2. The next scrape automatically omits LuLu from the rates list
   (no error card shown) — same state as before deploy.
3. Optional: delete the Worker from the Cloudflare dashboard.

## Cost

Zero. Cloudflare Workers free tier: 100 000 requests/day. The
hourly cron uses ~720/month. You will not see a bill.
