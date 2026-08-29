# GitHub Actions Auto Build & Release Guide — ScanFlow QR

Comprehensive guide to automated Continuous Integration (CI) and Continuous Delivery (CD) for **ScanFlow QR**.

---

## 🏗️ 1. Repository Structure

```text
ScanFlow-QR/
├── app/
├── gradle/
│   ├── wrapper/
│   │   ├── gradle-wrapper.jar
│   │   └── gradle-wrapper.properties
│   └── libs.versions.toml
├── .github/
│   └── workflows/
│       ├── android-ci.yml         # CI on push & PR (Unit Tests + Lint + Debug APK)
│       └── android-release.yml    # CD on tags/dispatch (Signed APK + AAB + GitHub Release)
├── docs/
│   ├── github-secrets.md
│   ├── github-build-release.md
│   ├── google-play-data-safety.md
│   ├── google-play-deployment.md
│   ├── test-plan.md
│   └── FINAL-RELEASE-REPORT.md
├── playstore/
│   ├── listing-details.md
│   └── asset-specifications.md
├── gradlew
├── gradlew.bat
├── gradle.properties
├── settings.gradle.kts
├── build.gradle.kts
├── README.md
├── CONTRIBUTING.md
├── SECURITY.md
├── LICENSE
└── .gitignore
```

---

## 🔄 2. CI Pipeline (`android-ci.yml`)

### When it runs:
- Push to `main` or `develop` branches.
- Any Pull Request targeted at `main`.

### Workflow Steps:
1. **Checkout Code**: Clones the latest commit.
2. **Setup Java 17**: Configures Temurin OpenJDK 17.
3. **Setup Gradle**: Uses `gradle/actions/setup-gradle@v4` with dependency caching.
4. **Unit Tests**: Runs `./gradlew testDebugUnitTest`.
5. **Android Lint**: Runs `./gradlew lintDebug`.
6. **Build Debug APK**: Runs `./gradlew assembleDebug`.
7. **Upload Artifact**: Stores `ScanFlow-QR-debug` in GitHub Actions artifacts for 14 days.

---

## 🚀 3. Release Pipeline (`android-release.yml`)

### When it runs:
- Push of any Git tag starting with `v` (e.g. `v1.0.0`, `v1.0.1`).
- Manual trigger via GitHub Actions **Run workflow** (`workflow_dispatch`).

### Workflow Steps:
1. **Decode Production Keystore**: Decodes `SIGNING_KEYSTORE_BASE64` into a temporary keystore file.
2. **Strict Validation**: Fails immediately if signing secrets are missing.
3. **Clean Build**: Executes `./gradlew clean`.
4. **Unit Tests**: Executes `./gradlew testReleaseUnitTest`.
5. **Build Release APK**: Executes `./gradlew assembleRelease`.
6. **Build Release AAB**: Executes `./gradlew bundleRelease`.
7. **Artifact Verification & Renaming**:
   - Renames APK to `ScanFlow-Qr-v<VERSION>-release.apk`.
   - Renames AAB to `ScanFlow-Qr-v<VERSION>-release.aab`.
   - Verifies file existence and non-zero size.
8. **Upload Artifacts**:
   - `ScanFlow-QR-APK` (30 days retention).
   - `ScanFlow-QR-AAB` (30 days retention).
9. **Publish GitHub Release**: Automatically generates a GitHub Release tagged `v<VERSION>` with changelog and attaches both the APK and AAB.
10. **Secure Cleanup**: Deletes the temporary keystore from the runner.

---

## 💻 4. Developer Commands & Release Flow

### Standard Development Flow (CI)
```bash
git add .
git commit -m "feat: enhance QR scanner detection frame"
git push origin main
```
> GitHub Actions automatically runs tests, lint, and produces `ScanFlow-QR-debug.apk`.

---

### Production Release Flow (CD)
```bash
# 1. Ensure you are on main branch with clean working tree
git checkout main
git pull origin main

# 2. Tag the version
git tag v1.0.0

# 3. Push the tag to GitHub
git push origin v1.0.0
```
> GitHub Actions automatically triggers `android-release.yml`, compiles signed artifacts, creates GitHub Release **ScanFlow QR v1.0.0**, and attaches:
> - `ScanFlow-Qr-v1.0.0-release.apk`
> - `ScanFlow-Qr-v1.0.0-release.aab`

---

## 📱 5. Testing & Deployment from Release

1. **Physical Android Device Smoke Test**:
   - Download `ScanFlow-Qr-v1.0.0-release.apk` from GitHub Releases.
   - Install via ADB:
     ```bash
     adb install -r ScanFlow-Qr-v1.0.0-release.apk
     ```
2. **Google Play Console Publication**:
   - Download `ScanFlow-Qr-v1.0.0-release.aab`.
   - Upload directly to **Google Play Console ➔ Production (or Closed Testing)**.

---

## 🛠️ 6. Troubleshooting

- **Build Fails with "Missing required secret SIGNING_KEYSTORE_BASE64"**:
  - Follow [docs/github-secrets.md](file:///d:/SMK%20Projek/ScanFlow%20QR/docs/github-secrets.md) to add the 4 required signing secrets to repository settings.
- **Unit Test Failure**:
  - Run `./gradlew testDebugUnitTest` locally to inspect test report in `app/build/reports/tests/testDebugUnitTest/`.
