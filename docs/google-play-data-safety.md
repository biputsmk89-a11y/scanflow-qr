# Google Play Data Safety Questionnaire Declarations

This document provides the exact answers and technical justification for the **Google Play Console Data Safety Form** for **ScanFlow QR (v1.0.0)**.

---

## 1. Overview of Data Collection

| Question | Answer | Technical Justification |
| :--- | :--- | :--- |
| **Does your app collect or share any user data?** | **No** | All barcode scanning, parsing, creation, and history storage are executed 100% locally on the user's device via Room Database and ML Kit on-device models. |
| **Is all user data encrypted in transit?** | **Not Applicable / Yes** | The app does not transmit user data to any external server or analytics cloud. When the user explicitly clicks an external link (e.g. opens a URL), system HTTPS handles standard browser encryption. |
| **Do you provide a way for users to request data deletion?** | **Yes** | Users can permanently delete individual scans or clear their entire database history at any time using the "Clear All History" feature in the History screen. |

---

## 2. Permissions Justification

| Permission | Purpose in ScanFlow QR | Is Runtime Requested? |
| :--- | :--- | :--- |
| `android.permission.CAMERA` | Real-time camera viewfinder for scanning QR codes and barcodes. | Yes (Requested on Scanner screen) |
| `android.permission.VIBRATE` | Tactile haptic feedback when a code is detected. | Normal permission |
| `android.permission.READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE` | Loading images from gallery to scan QR codes from photos. | Yes (Via Android Photo Picker / runtime prompt) |
| `android.permission.USE_BIOMETRIC` | Authenticating the user to unlock the app when App Lock is enabled. | Normal permission |

---

## 3. Third-Party Libraries & SDKs Audit

- **Google ML Kit Barcode Scanning**: Operates **entirely on-device**. No images or scan payloads are uploaded to Google servers.
- **ZXing Core**: Local Java/Kotlin bitmap generation algorithm. No network traffic.
- **AndroidX Room & DataStore**: Local SQLite and file storage on device internal sandbox.
- **Coil**: Local image rendering.

**Conclusion**: ScanFlow QR qualifies as a **Private, Zero-Data-Collection, Offline-First** application.
