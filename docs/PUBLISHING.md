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

The `app/build.gradle.kts` is wired to read keystore credentials from
either:
1. `android/keystore.properties` (preferred for local development)
2. environment variables `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`,
   `KEY_ALIAS`, `KEY_PASSWORD` (preferred for CI)

If neither is present, release builds fall back to the debug keystore
so the build doesn't fail outright (NOT acceptable for actual release —
just for first-time setup).

### Generate the keystore (one-time)

```bash
cd android
keytool -genkey -v \
  -keystore release.jks \
  -alias transferrate \
  -keyalg RSA -keysize 4096 \
  -validity 10000 \
  -dname "CN=Transfer Rate, OU=Open Source, O=imraneggy, L=Dubai, C=AE"
```

Then create `android/keystore.properties` (copy from
`keystore.properties.example`):

```
storeFile=release.jks
storePassword=YOUR_KEYSTORE_PASSWORD
keyAlias=transferrate
keyPassword=YOUR_KEY_PASSWORD
```

- [ ] **Do not** commit `release.jks` or `keystore.properties` —
      both are gitignored.
- [ ] Back up the keystore + passwords in two places (password manager +
      offline copy). Losing this means losing the ability to push
      updates to existing installs.

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

The repo already contains the metadata structure F-Droid expects:

```
fastlane/metadata/android/en-US/
├── title.txt
├── short_description.txt        (max 80 chars)
├── full_description.txt         (max 4000 chars)
└── changelogs/
    └── 8.txt                    (per-versionCode changelog)
```

When making a new release:
- [ ] Add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`
      with the user-facing changes.
- [ ] Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
- [ ] Tag the release in git (`git tag v0.8.1 && git push --tags`).

To submit:
- [ ] Fork [fdroiddata](https://gitlab.com/fdroid/fdroiddata).
- [ ] Add `metadata/com.transferrate.app.yml`:
      ```yaml
      Categories: [Money]
      License: MIT
      AuthorName: imraneggy
      WebSite: https://github.com/imraneggy/transfer-rate
      SourceCode: https://github.com/imraneggy/transfer-rate
      IssueTracker: https://github.com/imraneggy/transfer-rate/issues
      AutoName: Transfer Rate
      RepoType: git
      Repo: https://github.com/imraneggy/transfer-rate.git
      Builds:
        - versionName: 0.8.1
          versionCode: 8
          commit: v0.8.1-alpha
          subdir: android
          gradle:
            - yes
      AutoUpdateMode: Version
      UpdateCheckMode: Tags
      CurrentVersion: 0.8.1
      CurrentVersionCode: 8
      ```
- [ ] Open an MR. F-Droid maintainers review and either approve or
      request changes. Build is reproducible from your repo.

F-Droid will rebuild from source on their infrastructure — they
don't trust your pre-built APK. Make sure `./gradlew :app:assembleRelease`
works from a clean clone with only public dependencies.

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
