# ScanFlow QR
**Scan. Create. Connect.**

A production-ready, native Android application built with **Jetpack Compose**, **Material 3**, and **Clean Architecture (MVVM)**, designed using the **Google Stitch MCP Design System**.

---

## 🚀 Quick Start & Build Commands

### Prerequisites
- **JDK 17+** (Recommended: OpenJDK 17)
- **Android SDK** (`compileSdk = 34`, `targetSdk = 34`, `minSdk = 24`)
- **Android Studio Hedgehog / Iguana / Jellyfish (2023.1.1+)**

### Local Build & Testing Commands
```bash
# 1. Run All Unit Tests
./gradlew testDebugUnitTest       # On Windows: .\gradlew testDebugUnitTest

# 2. Run Android Lint Checks
./gradlew lintDebug               # On Windows: .\gradlew lintDebug

# 3. Build Debug APK (For Local Device Testing)
./gradlew assembleDebug           # On Windows: .\gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# 4. Build Production Release APK (Minified via R8)
./gradlew assembleRelease         # On Windows: .\gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk

# 5. Build Production Android App Bundle (AAB for Google Play Console)
./gradlew bundleRelease           # On Windows: .\gradlew bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab
```

### Install Directly to Physical Android Device via ADB
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🌟 Core Features

- **Real CameraX & ML Kit Multi-Format Scanner**:
  - Live scanning supporting 12+ symbologies (`QR_CODE`, `AZTEC`, `DATA_MATRIX`, `PDF_417`, `CODE_128`, `CODE_39`, `CODE_93`, `EAN_13`, `EAN_8`, `ITF`, `UPC_A`, `UPC_E`).
  - Scan directly from gallery photos using Android Photo Picker.
  - Flashlight toggle, camera flip (Back/Front), animated glowing cyan laser overlay, and duplicate scan prevention.
  - Tactile haptic vibration and audio feedback.

- **Intelligent QR Parser & Security Rating Engine**:
  - Automatically classifies into 11 categories: **Website / URL**, **Wi-Fi**, **Contact (vCard)**, **Email**, **Phone**, **SMS**, **Location (Geo)**, **Calendar (vEvent)**, **Payment (UPI/Crypto/PayPal)**, **Social Media**, and **Plain Text**.
  - **Zero-Auto-Execute Security Rule**: Never executes payloads without user consent.
  - **Heuristic URL Security Checker**: Analyzes HTTPS protocol encryption, direct IP hostnames, suspicious TLDs, and dangerous schemes (`javascript:`, `data:`).
  - Dynamic Action Buttons: Open Browser, Dial Phone, Compose Email, Send SMS, Open Maps, Save Contact, Connect Wi-Fi, Copy, and Share.

- **Pro QR Studio & Deep Customization**:
  - Generates 11 types of QR codes with real-time input validation.
  - Custom foreground and background colors, dot pattern styles (*Square, Rounded, Dots, Diamond*), and corner eye styles (*Square, Rounded, Circle*).
  - High-res PNG export to MediaStore (`Pictures/ScanFlowQR`) and Android Sharesheet via `FileProvider`.

- **Offline-First Persistence (Room Database & DataStore)**:
  - Full scan history with instant search, category filters, single delete, batch delete, and clear all.
  - User-created QR library with duplicate, share, and local analytics counters (scans, shares, downloads).
  - Pinned Favorites synchronized reactively via Kotlin Flow.
  - DataStore Preferences for Theme Mode (Dark/Light/System), haptics, and auto-copy/auto-open toggles.

- **Biometric Security & App Lock**:
  - AndroidX `BiometricPrompt` supporting Fingerprint & Face Unlock.
  - Fallback 4-digit PIN setup.
  - Zero-knowledge local offline storage vault.

---

## 🎨 Google Stitch MCP Design System

Designed with Google Stitch MCP (`projects/17161269787102953129`):
- **Primary Color**: Electric Blue (`#0052FF`)
- **Secondary / Accent**: Cyan Glow (`#00E5FF`)
- **Dark Background**: `#0B1020`
- **Dark Surface**: `#131B2E`
- **Light Background**: `#F7F9FC`
- **Light Surface**: `#FFFFFF`
- **Success / Warning / Error**: `#00C853`, `#FFAB00`, `#FF3B30`
- **Corner Radii**: 16dp – 24dp rounded cards and minimum 48dp touch targets.

---

## 🏗️ Clean Architecture & Project Structure

```
ScanFlow QR/
├── app/
│   ├── build.gradle.kts           # App build configuration with release signing & R8
│   ├── proguard-rules.pro         # ProGuard/R8 optimization rules
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── res/               # Colors, Strings, Themes, FileProvider paths
│       │   └── java/com/scanflow/qr/
│       │       ├── ScanFlowApplication.kt # Dependency registry & database builder
│       │       ├── MainActivity.kt        # Theme controller & biometric lock
│       │       ├── core/                  # DesignSystem, Navigation, Database, Security, Utils
│       │       ├── data/                  # Room Entities, DAOs, Repositories, Mappers
│       │       ├── domain/                # Models, Repository Interfaces, UseCases
│       │       └── feature/               # 16 Compose Feature Modules
│       └── test/java/com/scanflow/qr/     # Comprehensive Unit Tests Suite
├── .github/
│   └── workflows/
│       ├── android-ci.yml         # Automated CI (Test, Lint, Debug APK)
│       └── android-release.yml    # Automated Release Pipeline (Signed APK & AAB)
├── docs/
│   ├── FINAL-BUILD-VERIFICATION.md # Final build verification logs
│   ├── FINAL-RELEASE-REPORT.md    # Final release audit certification
│   ├── github-build-release.md    # Guide for GitHub CI/CD build & release
│   ├── github-secrets.md          # Guide for configuring GitHub Repository secrets
│   ├── google-play-data-safety.md # Play Console Data Safety questionnaire declarations
│   ├── google-play-deployment.md  # Step-by-step submission & release tracks guide
│   ├── release-checklist.md       # Pre-release quality gate checklist
│   └── test-plan.md               # Master QA test matrix
├── playstore/
│   ├── listing-details.md         # Store listing metadata (short/full descriptions)
│   ├── asset-specifications.md    # Icon, Feature graphic, and Screenshot specs
│   └── icon_512.png               # High-res 512x512 app icon asset
├── CONTRIBUTING.md                # Contribution guidelines
├── SECURITY.md                    # Vulnerability reporting policy
├── LICENSE                        # Apache 2.0 License
├── .gitignore                     # Production Git exclusion rules
└── README.md
```

---

## 🤖 GitHub Actions CI/CD Pipeline

- **Continuous Integration (`.github/workflows/android-ci.yml`)**:
  - Automatically triggered on `push` and `pull_request` to `main`.
  - Runs unit tests, lint checks, builds debug APK, and uploads artifact.
- **Production Release Pipeline (`.github/workflows/android-release.yml`)**:
  - Automatically triggered when a Git tag is pushed (e.g. `v1.0.0`) or via manual trigger.
  - Restores release keystore from GitHub Secrets (`SIGNING_KEYSTORE_BASE64`), compiles minified `app-release.apk` and `app-release.aab`, and publishes a GitHub Release.

---

## 🔒 Security & Privacy Guarantee
All QR codes and barcodes scanned or created in **ScanFlow QR** are processed 100% locally on your device. Zero user data or scanned payloads are transmitted to external servers.
