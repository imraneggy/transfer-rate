# Privacy policy

**Effective date: 2026-06-16**

## Short version

Transfer Rate does not collect, store, transmit, or share any personal
information about you. It is ad-free and requires no account. Personalization
(display name, favourite providers, preferred amount) is stored locally on your
device only and never leaves it.

## Detail

### What we collect

Nothing. There is no analytics SDK, no crash reporter, no advertising ID read,
no device fingerprinting, no advertising network, and no account.

### What is transmitted from your device

**Rate data fetch:**
A single HTTP `GET` to `https://imraneggy.github.io/transfer-rate/rates.json`
each time you open the app or tap Refresh. The request includes:

* Your device's IP address (visible to GitHub Pages and any CDN node it
  passes through — the same as any web request).
* The User-Agent string `TransferRateApp/<version> Android`.

We do not log or process this traffic; GitHub's standard
[Privacy Statement][gh-privacy] covers their handling.

**Manual refresh trigger (optional):**
If you tap the Refresh button and the app triggers an upstream scrape, a POST
request is sent to a Cloudflare Worker at
`https://transfer-rate-refresh.imranbatchait.workers.dev`. This carries a
short bearer token embedded in the APK and nothing else — no user data, no
device identifiers. Cloudflare's [Privacy Policy][cf-privacy] applies to this
request. The Cloudflare Worker then dispatches a GitHub Actions workflow to
publish fresh rates; it does not log user data.

**Google Play Billing (Pro subscribers only):**
If you purchase Transfer Rate Pro, the purchase is processed entirely by Google
Play. We receive only a purchase token from the Play Billing API to verify that
your subscription is active. We never see your payment details. Google's
[Privacy Policy][google-privacy] applies to the purchase flow.

### Permissions

* `INTERNET` — to fetch the rates JSON and (optionally) trigger a manual
  refresh via the Cloudflare Worker.
* `ACCESS_NETWORK_STATE` — to check connectivity before attempting a fetch.
* `POST_NOTIFICATIONS` — requested once, only if you have enabled daily-high
  alerts in **About → Notifications**. You can deny or revoke it at any time
  from Android Settings → Apps → Transfer Rate → Notifications without
  breaking any other feature.

The app declares `RECEIVE_BOOT_COMPLETED` to reschedule the background
rate-prefetch job after device restart; no data is read or transmitted during
that boot event beyond what WorkManager itself needs internally.

### Local data (personalization)

The following is stored only on your device in Android SharedPreferences:

* Your display name (optional).
* Preferred sending amount (default: AED 1,000).
* Preferred currency corridor (default: INR).
* Favourite providers list.
* Notification preferences (daily-high toggle, rate-target threshold).
* Onboarding-tour dismissal flag.
* Pro subscription state cache (verified against Google Play on each launch).

None of this data is transmitted anywhere. Android's standard app-data backup
(`android:allowBackup`) is **disabled** in the manifest so it never leaves the
device through backup channels either.

### Data shared with third parties

None. The only external services involved are:
* **GitHub Pages** — serves the public rates JSON.
* **Cloudflare Workers** — optional manual-refresh trigger (no user data).
* **Google Play Billing** — processes Pro subscription payments.

### Children

The app is not directed at children under 13.

### Changes

We will update this document and the effective date whenever the privacy posture
changes. Significant changes will be noted in the app's release notes.

### Contact

Email: **imranbatchait@gmail.com**

Or open an issue in the public GitHub repository.

[gh-privacy]: https://docs.github.com/en/site-policy/privacy-policies/github-general-privacy-statement
[cf-privacy]: https://www.cloudflare.com/privacypolicy/
[google-privacy]: https://policies.google.com/privacy
