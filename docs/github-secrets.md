# GitHub Secrets Configuration Guide — ScanFlow QR

This guide explains how to configure GitHub Secrets to enable automated, secure Android App signing for APK and AAB releases via GitHub Actions.

---

## 🔐 Required GitHub Secrets

Navigate to your GitHub repository:  
**Settings** ➔ **Secrets and variables** ➔ **Actions** ➔ **New repository secret**

| Secret Name | Description | Example / Value Source |
| :--- | :--- | :--- |
| `SIGNING_KEYSTORE_BASE64` | Base64-encoded string of your production `.jks` or `.keystore` file. | Output of `base64` command (see below) |
| `KEYSTORE_PASSWORD` | Password protecting the keystore file. | Value entered when creating keystore |
| `KEY_ALIAS` | Alias name of the signing key inside the keystore. | e.g. `scanflow-release-key` |
| `KEY_PASSWORD` | Password protecting the specific key alias. | Value entered for key password |

---

## 🛠️ Step-by-Step Keystore Generation & Base64 Encoding

### 1. Generate Production Keystore (If you don't have one)
Run this command in terminal:
```bash
keytool -genkey -v -keystore scanflow-release.jks -alias scanflow-release-key -keyalg RSA -keysize 2048 -validity 10000
```

### 2. Encode Keystore to Base64

#### On Linux / macOS / Git Bash:
```bash
base64 -w 0 scanflow-release.jks > keystore_base64.txt
```

#### On Windows PowerShell:
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("scanflow-release.jks")) | Out-File -Encoding ASCII keystore_base64.txt
```

### 3. Add to GitHub Secrets
1. Open `keystore_base64.txt` and copy the entire continuous string.
2. In GitHub, create the secret `SIGNING_KEYSTORE_BASE64` and paste the string.
3. Add `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`.
4. Delete `keystore_base64.txt` and store `scanflow-release.jks` in a secure offline vault.

> [!CAUTION]
> **NEVER** commit `scanflow-release.jks` or `keystore_base64.txt` to git.
