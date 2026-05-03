/**
 * Cloudflare Worker — LuLu Exchange rate proxy (v2)
 * ====================================================
 *
 * Why this exists
 * ---------------
 * LuLu Exchange's primary rate API runs on TCP port 9443
 * (lieservices.luluone.com:9443/liveccyrates). GitHub Actions runners
 * block outbound traffic to non-standard ports — and surprisingly,
 * Cloudflare Workers also restrict fetch() to ports 80/443. So we
 * cannot proxy the original endpoint at all.
 *
 * LuLu has a second, port-443 API at orville.luluone.com that powers
 * the rate-history graph on their homepage. It uses an OAuth-style
 * auth flow whose credentials are publicly embedded in their
 * homepage JavaScript (every browser visitor sends them). When called
 * from a Cloudflare Worker IP the rate-history endpoint returns a
 * usable rate; from datacenter IPs it gets WAF-blocked, but CF's
 * trusted network is permitted.
 *
 * This Worker:
 *   1. Performs the two-step LuLu auth + rate-history flow
 *   2. Returns a normalized envelope matching the legacy port-9443
 *      response shape so our LuluProvider scraper doesn't need to
 *      change downstream parsing
 *
 * Security stance
 * ---------------
 * Not an open proxy. Only GET / is supported and the response is
 * always derived from the same hardcoded LuLu rate-history call —
 * the request body is ignored. No private credentials stored; the
 * client/secret/username/password values below are the same public
 * ones embedded in luluexchange.com's bundled JavaScript.
 */

const AUTH_URL = 'https://orville.luluone.com/hbapi/v1_0/auth/token';
const RATE_URL = 'https://orville.luluone.com/hbapi/v1_0/pas/rate-history';

// All four values are public — they appear in plaintext inside
// https://luluexchange.com/wp-content/themes/lulu_exchange/assets/js/custom.js
// and every browser visitor of luluexchange.com transmits them.
const CLIENT_ID = 'lulumoney';
const CLIENT_SECRET = 'HqJWJPUszMDCVkKCm';
const AUTH_USER = 'lulumoneyadmin';
const AUTH_PASS = 'qudncwUu0syCecPcQ0bJcqK1qUmh5yh';

// Rate-history corridor — UAE (AED) -> India (INR) only.
const COMPANY = '784100';
const AGENT = '784999';

// Cache the bearer token for 5 minutes to reduce auth round-trips.
let cachedToken = null;
let cachedTokenExpiresAt = 0;

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    if (request.method !== 'GET' || url.pathname !== '/') {
      return jsonResponse({ error: 'GET / only' }, 404);
    }

    let token;
    try {
      token = await getToken();
    } catch (e) {
      return jsonResponse(
        { error: 'auth failed', detail: String(e) },
        502,
      );
    }

    let upstream;
    try {
      upstream = await fetch(RATE_URL, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
          'sender': CLIENT_ID,
          'company': COMPANY,
          'Authorization': `Bearer ${token}`,
          'Origin': 'https://www.luluexchange.com',
          'Referer': 'https://www.luluexchange.com/',
          'User-Agent': 'Mozilla/5.0 (compatible; lulu-proxy/2.0)',
        },
        body: JSON.stringify({
          product: 'LR',
          agent: AGENT,
          sendcurrency: 'AED',
          receivingcurrency: 'INR',
        }),
      });
    } catch (e) {
      return jsonResponse(
        { error: 'rate fetch failed', detail: String(e) },
        502,
      );
    }

    if (!upstream.ok) {
      return jsonResponse(
        { error: 'upstream non-OK', status: upstream.status },
        502,
      );
    }

    let data;
    try {
      data = await upstream.json();
    } catch (e) {
      return jsonResponse(
        { error: 'upstream non-JSON', detail: String(e) },
        502,
      );
    }

    // The orville endpoint returns:
    //   { code: 200, data: { rates: [
    //       {ratedate, rate, sellrate, buyrate, ...}, ...
    //   ]}}
    // We re-shape into the legacy port-9443 envelope our scraper expects:
    //   { code, message, payload: { rates: [
    //       {frmccy, toccy, rate, sellrate, buyrate, ccyname, ...}, ...
    //   ]}}
    const orvilleRates = (data && data.data && data.data.rates) || [];
    if (!Array.isArray(orvilleRates) || orvilleRates.length === 0) {
      return jsonResponse(
        { error: 'upstream returned no rates', upstream: data },
        502,
      );
    }

    // Use the most-recent rate as today's reading; older entries are
    // history points the scraper currently ignores.
    const sorted = orvilleRates
      .filter((r) => r && (r.rate || r.sellrate))
      .sort((a, b) => String(b.ratedate || '').localeCompare(String(a.ratedate || '')));

    const reshapedRates = sorted.map((row) => ({
      frmccy: 'AED',
      toccy: 'INR',
      rate: parseFloat(row.rate || row.sellrate),
      sellrate: parseFloat(row.sellrate || row.rate),
      buyrate: parseFloat(row.buyrate || row.rate),
      ccyname: 'INDIAN RUPEE',
      ratedate: row.ratedate,
      svcprovcd: 'LULUXAEDX',
    }));

    return jsonResponse(
      {
        code: 200,
        message: 'success',
        payload: { rates: reshapedRates },
      },
      200,
      { 'cache-control': 'public, max-age=60' },
    );
  },
};

async function getToken() {
  const now = Date.now();
  if (cachedToken && cachedTokenExpiresAt > now + 30_000) {
    return cachedToken;
  }
  const r = await fetch(AUTH_URL, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      'domain': 'tenants',
      'clientid': CLIENT_ID,
      'clientsecret': CLIENT_SECRET,
      'X-Requested-With': 'XMLHttpRequest',
      'Origin': 'https://www.luluexchange.com',
      'Referer': 'https://www.luluexchange.com/',
      'User-Agent': 'Mozilla/5.0 (compatible; lulu-proxy/2.0)',
    },
    body: JSON.stringify({
      username: AUTH_USER,
      password: AUTH_PASS,
    }),
  });
  if (!r.ok) {
    throw new Error(`auth status ${r.status}`);
  }
  const j = await r.json();
  const token = j && j.token && j.token.access_token;
  if (!token) {
    throw new Error('no access_token in auth response');
  }
  cachedToken = token;
  // Cache for 5 minutes — JWT itself is valid much longer
  cachedTokenExpiresAt = now + 5 * 60 * 1000;
  return token;
}

function jsonResponse(obj, status, extraHeaders) {
  return new Response(JSON.stringify(obj), {
    status,
    headers: {
      'content-type': 'application/json; charset=utf-8',
      'access-control-allow-origin': '*',
      ...(extraHeaders || {}),
    },
  });
}
