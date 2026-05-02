/**
 * Cloudflare Worker — LuLu Exchange rate proxy
 * ==============================================
 *
 * Why this exists
 * ---------------
 * LuLu Exchange's live-rate API (lieservices.luluone.com:9443/liveccyrates)
 * runs on a non-standard TCP port (9443). GitHub Actions datacenter
 * runners block outbound traffic to non-standard ports, so the LuLu
 * scraper times out in CI. This Worker:
 *   1. Listens on standard 443 (the Worker's own URL)
 *   2. Forwards a single, narrowly-defined request to LuLu's API
 *   3. Returns LuLu's JSON response unchanged
 *
 * Security stance
 * ---------------
 * This is intentionally NOT an open proxy. It only forwards one specific
 * request signature (the public liveccyrates GET with the documented
 * payload). Any request that doesn't match is rejected with 403.
 * The Gravitee API key bundled in the upstream call is the same key
 * every visitor's browser sends — it's public by design.
 *
 * Deployment
 * ----------
 *   1. Create a free Cloudflare account (cloudflare.com).
 *   2. In the dashboard, Workers & Pages → Create → Worker.
 *   3. Replace the default code with this file's contents and Deploy.
 *   4. Note the worker URL (e.g. lulu-proxy.<your-subdomain>.workers.dev).
 *   5. Add it as a repo secret named `LULU_PROXY_URL` in GitHub.
 *   6. Re-run the scrape workflow; LuLu will now appear in the rates list.
 *
 * Free tier comfortably covers 1 cron run/hour × 24h × 30d ≈ 720 calls/mo.
 * (Cloudflare's free tier allows 100 000 requests/day.)
 */

const ALLOWED_AGLCID = 784278;          // UAE location id
const UPSTREAM_HOST = 'lieservices.luluone.com';
const UPSTREAM_PORT = '9443';
const UPSTREAM_PATH = '/liveccyrates';
// This API key is also embedded in LuLu's public website JavaScript and
// is sent by every browser visitor. Treating it as public is correct.
const GRAVITEE_API_KEY = '94cfe79a-ec6a-4f11-96c1-d12a928ad3f1';

const ALLOWED_INSTYPES = new Set(['LR']);
const RATE_LIMIT_MAX_PER_MINUTE = 30;

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    // Only support GET on the root path.
    if (request.method !== 'GET' || url.pathname !== '/') {
      return new Response('Not found', { status: 404 });
    }

    // Validate the payload query param. Only allow the exact shape we
    // expect for the LuLu rate fetch.
    const payloadStr = url.searchParams.get('payload');
    if (!payloadStr) {
      return jsonResponse(
        { error: 'missing payload query parameter' },
        400,
      );
    }

    let payload;
    try {
      payload = JSON.parse(payloadStr);
    } catch (e) {
      return jsonResponse({ error: 'invalid JSON in payload' }, 400);
    }

    if (
      payload.activityType !== 'rates.get' ||
      payload.aglcid !== ALLOWED_AGLCID ||
      !ALLOWED_INSTYPES.has(payload.instype)
    ) {
      return jsonResponse(
        { error: 'unsupported payload — only AED-corridor rates.get is proxied' },
        403,
      );
    }

    // Forward to LuLu's port-9443 endpoint.
    const upstreamUrl =
      `https://${UPSTREAM_HOST}:${UPSTREAM_PORT}${UPSTREAM_PATH}` +
      `?payload=${encodeURIComponent(JSON.stringify(payload))}`;

    let upstreamResponse;
    try {
      upstreamResponse = await fetch(upstreamUrl, {
        method: 'GET',
        headers: {
          'x-gravitee-api-key': GRAVITEE_API_KEY,
          'Origin': 'https://www.luluexchange.com',
          'Referer': 'https://www.luluexchange.com/',
          'User-Agent': 'Mozilla/5.0 (compatible; transfer-rate-bot/1.0)',
          'Accept': 'application/json',
        },
        // Cloudflare Workers default to a 30-second subrequest timeout.
        cf: { cacheTtl: 60, cacheEverything: false },
      });
    } catch (e) {
      return jsonResponse(
        { error: 'upstream fetch failed', detail: String(e) },
        502,
      );
    }

    if (!upstreamResponse.ok) {
      return jsonResponse(
        { error: 'upstream returned non-OK', status: upstreamResponse.status },
        502,
      );
    }

    const body = await upstreamResponse.text();
    return new Response(body, {
      status: 200,
      headers: {
        'content-type': 'application/json; charset=utf-8',
        'cache-control': 'public, max-age=60',
        'access-control-allow-origin': '*',
      },
    });
  },
};

function jsonResponse(obj, status) {
  return new Response(JSON.stringify(obj), {
    status,
    headers: { 'content-type': 'application/json; charset=utf-8' },
  });
}
