# Release Engineering Checklist — ScanFlow QR (v1.0.0)

Quality gate and pre-submission checklist for Google Play and GitHub release.

---

## 📋 Pre-Release Verification Checklist

### 1. Code & Architecture
- [x] Full native Android Kotlin codebase (Zero web wrappers / Zero React Native).
- [x] Clean Architecture layer boundaries preserved (Core / Data / Domain / Feature).
- [x] No `TODO`, `FIXME`, or mocked/fake placeholders in production code.
- [x] Zero hardcoded secrets, API keys, or private keystores committed to VCS.

### 2. Gradle & Dependencies
- [x] `compileSdk = 34`, `targetSdk = 34`, `minSdk = 24`.
- [x] Gradle Version Catalog (`libs.versions.toml`) contains stable compatible versions.
- [x] ProGuard/R8 minification and resource shrinking enabled for `release` buildType.
- [x] Dynamic signing configuration configured with local fallback and CI secret support.

### 3. UI/UX & Design System
- [x] Google Stitch MCP design system tokens applied (Electric Blue `#0052FF`, Cyan `#00E5FF`, Navy `#131B2E`).
- [x] Dark Mode and Light Mode tested and validated across all 16 feature screens.
- [x] Touch targets comply with accessibility standard (minimum 48dp).
- [x] Content scaling validated down to 360dp width screen devices.

### 4. Quality Assurance & Testing
- [x] Unit test suite passed (`QrCodeParserTest`, `UrlSecurityCheckerTest`, `DataMappersTest`).
- [x] CameraX and ML Kit tested across 12+ barcode symbologies.
- [x] 11 QR generator formats verified for payload standard compliance.
- [x] Scoped Storage MediaStore PNG export and FileProvider sharesheet tested.

### 5. Security & Privacy
- [x] Untrusted QR input handling strictly enforced (Zero auto-execute).
- [x] URL security heuristics (HTTPS verification, IP host check, suspicious TLD flag) verified.
- [x] AndroidX BiometricPrompt fingerprint and PIN lock verified.
- [x] Local-only Data Safety declarations completed in `docs/google-play-data-safety.md`.

### 6. Play Store & CI/CD
- [x] Play Store short and full descriptions prepared in `playstore/listing-details.md`.
- [x] Graphic asset specifications documented in `playstore/asset-specifications.md`.
- [x] GitHub Actions CI workflow created (`.github/workflows/android-ci.yml`).
- [x] GitHub Actions Release pipeline created (`.github/workflows/android-release.yml`).
- [x] Comprehensive `.gitignore` protecting secrets, keystores, and build outputs.
