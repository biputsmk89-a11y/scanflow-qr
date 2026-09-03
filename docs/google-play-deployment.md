# Google Play Console Deployment Guide — ScanFlow QR

Step-by-step procedure for deploying **ScanFlow QR** from local repository to Google Play Console.

---

## 1. Prerequisites
1. **Google Play Developer Account** ($25 one-time registration fee).
2. **Release Keystore** generated and stored in a secure offline vault.
3. **Signed AAB Artifact** (`app/build/outputs/bundle/release/app-release.aab`).

---

## 2. Generating Release Keystore

Run the following command in terminal to create a release keystore:
```bash
keytool -genkey -v -keystore scanflow-release.jks -alias scanflow-key -keyalg RSA -keysize 2048 -validity 10000
```
> [!CAUTION]
> **NEVER commit `scanflow-release.jks` to GitHub!** Keep multiple encrypted backups of this file.

---

## 3. Configuring GitHub Actions Secrets for Automated Releases

To enable automated release builds via `.github/workflows/android-release.yml`, add the following Secrets to your GitHub repository (**Settings > Secrets and variables > Actions**):

| Secret Name | Description | Example / Value |
| :--- | :--- | :--- |
| `SIGNING_KEYSTORE_BASE64` | Base64 encoded string of `scanflow-release.jks` | `base64 -w 0 scanflow-release.jks` |
| `SIGNING_KEYSTORE_PASSWORD` | Password of the keystore file | `YourKeystorePassword` |
| `SIGNING_KEY_ALIAS` | Key alias name | `scanflow-key` |
| `SIGNING_KEY_PASSWORD` | Password for the key alias | `YourKeyPassword` |

---

## 4. Release Track Progression

### Track 1: Internal Testing
- Upload the signed `app-release.aab`.
- Add internal QA testers by email.
- Validate installation, CameraX scanner on real physical devices, and Room persistence across app updates.

### Track 2: Closed Testing (Alpha / Beta)
- 20+ testers test for 14 consecutive days (Google Play requirement for personal developer accounts).
- Gather crash logs and ANR metrics via Play Console Android Vitals.

### Track 3: Production Track
- Promote tested release to **Production**.
- Roll out in stages (e.g. 10% -> 25% -> 50% -> 100%) to monitor stability.
