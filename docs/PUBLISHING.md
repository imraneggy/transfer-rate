# Publishing checklist (open source + Android)

A pragmatic checklist for taking this project public. Items marked
**[free]** require no spending; **[$25]** is the one-time Google Play
developer registration if you want Play Store distribution.

## 1. GitHub repository setup [free]

- [ ] Create the repo at `github.com/<you>/transfer-rate` as **public**.
- [ ] Push the local repo:
      ```bash
      git remote add origin git@github.com:<you>/transfer-rate.git
      git push -u origin main
      ```
- [ ] Settings → **Pages** → Source: GitHub Actions.
- [ ] Settings → **Actions** → General → "Read and write permissions"
      for GITHUB_TOKEN (workflows already grant minimum needed; this is
      a defence-in-depth check).
- [ ] Settings → **Branches** → Add a rule for `main`:
      - Require PR before merge
      - Require status checks (`scrape` once it has run once)
      - Disallow force-push
- [ ] Add a description, topics (`uae`, `remittance`, `inr`, `aed`,
      `android`), and a homepage URL pointing to the Pages site.
- [ ] Create a `CITATION.cff` if you'd like academic-style citations
      (optional).

## 2. Verify the data plane

- [ ] First scheduled run succeeds. Check:
      ```
      curl https://<you>.github.io/transfer-rate/rates.json | jq .
      ```
- [ ] Replace `imraneggy.github.io` in `RatesRepository.kt` and
      `network_security_config.xml` if your username differs.

## 3. Android signing key [free]

Generate once, keep safe:

```bash
keytool -genkey -v -keystore release.keystore \
  -alias transferrate -keyalg RSA -keysize 4096 -validity 10000
```

- [ ] **Do not** commit `release.keystore`.
- [ ] Back up the keystore + passwords in two places (password manager +
      offline copy). Losing this means losing the ability to push
      updates to existing installs.
- [ ] Set environment variables when building releases:
      `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

## 4. Build a release APK [free]

```bash
cd android
./gradlew :app:assembleRelease
# output: app/build/outputs/apk/release/app-release.apk
```

Verify:

```bash
# Confirm minSdk / targetSdk
aapt dump badging app/build/outputs/apk/release/app-release.apk | head
# Should print: sdkVersion:'34' targetSdkVersion:'34'
```

## 5. Distribution options

### a. GitHub Releases (free, simplest)

- [ ] Tag a version: `git tag v0.1.0 && git push --tags`
- [ ] Create a Release in GitHub UI; attach the signed APK.
- [ ] Users sideload the APK. Educate via README that they need
      "Install unknown apps" enabled for their browser.

### b. F-Droid (free, auditable)

- [ ] Submit a metadata yaml at the
      [F-Droid Data repo](https://gitlab.com/fdroid/fdroiddata).
- [ ] F-Droid builds your app from source — they don't accept the
      pre-built APK. Make sure `./gradlew :app:assembleRelease` works
      from a clean clone with only public dependencies.
- [ ] Reproducible-builds compliance is a plus.

### c. Google Play [$25 one-time]

- [ ] Pay the one-time $25 Play Console fee.
- [ ] Build an AAB instead of an APK:
      ```bash
      ./gradlew :app:bundleRelease
      ```
- [ ] Enroll in Play App Signing (Google holds the upload key, you
      keep the version key). This protects you if your keystore is
      ever lost.
- [ ] Fill in the **Data safety** form: declare "no data collected"
      (truthful — see `PRIVACY.md`).
- [ ] Privacy policy URL: link to the `PRIVACY.md` rendered by GitHub
      (`https://github.com/<you>/transfer-rate/blob/main/PRIVACY.md`).
- [ ] App content rating: Everyone.
- [ ] Target audience: 18+ (financial info), no children.
- [ ] Categorise as Finance.

## 6. Trademark / legal hygiene

- [ ] Do not use any provider's logo in screenshots or marketing.
- [ ] Use plain text provider names only.
- [ ] Disclaimer prominently in-app (already present, footer of list).
- [ ] Takedown contact in README and TAKEDOWN.md.

## 7. Community files

- [ ] `CODE_OF_CONDUCT.md` (already created).
- [ ] `CONTRIBUTING.md` (already created).
- [ ] `SECURITY.md` (already created).
- [ ] Issue templates (optional): create
      `.github/ISSUE_TEMPLATE/bug_report.yml` and
      `feature_request.yml`.
- [ ] PR template at `.github/pull_request_template.md` summarising
      "what changed, how tested, any provider-specific notes."

## 8. Observability (optional, free)

- [ ] Subscribe to your own Actions failure emails.
- [ ] Add a simple shields.io badge to README so you (and the world)
      see at a glance whether scrapes are healthy.
- [ ] Pin a permanent issue listing known broken scrapers, updated by
      the maintainer.

## 9. Versioning

Use semver-ish:

* `0.x` while still moving fast on the JSON schema.
* `1.0` once the JSON schema is stable and you commit to backwards
  compatibility for one major version.
* Bump `versionCode` and `versionName` in
  `android/app/build.gradle.kts` for every Play release.

## 10. Post-launch

- [ ] Watch the Actions tab daily for the first week.
- [ ] Monitor for wrong-rate issues — react fast; trust is hard to
      regain.
- [ ] Cycle the README's "Providers" table when scrapers move between
      `working` / `investigating` / `removed`.
