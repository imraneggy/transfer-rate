# Privacy policy

**Effective date: 2026-05-01**

## Short version

This app does not collect, store, transmit, or share any personal information
about you. It makes one HTTP request per refresh to a public file on
GitHub Pages and displays the result. That's it.

## Detail

### What we collect

Nothing. There is no analytics SDK, no crash reporter, no advertising ID
read, no device fingerprinting, no account.

### What is transmitted from your device

A single HTTP `GET` to `https://imraneggy.github.io/transfer-rate/rates.json`
each time you open the app or tap refresh. The request includes:

* Your device's IP address (visible to GitHub Pages and any CDN node it
  passes through, the same as any web request)
* The User-Agent string `TransferRateApp/<version> Android`

We do not log or process this traffic; GitHub's standard
[Privacy Statement][gh-privacy] covers their handling.

### Permissions

* `INTERNET` — to fetch the rates JSON. No other permissions are requested.

### Data shared with third parties

None.

### Children

The app is not directed at children under 13.

### Changes

We will update this document and the effective date if the privacy posture
ever changes.

### Contact

Open an issue or discussion in the GitHub repository.

[gh-privacy]: https://docs.github.com/en/site-policy/privacy-policies/github-general-privacy-statement
