# Privacy Policy for ScanFlow QR

*Last updated: September 20, 2026*

ScanFlow QR ("we", "our", or "the App") is developed as a utility application designed to respect and protect your privacy. This Privacy Policy explains our practices regarding the collection, use, and disclosure of information when you use our mobile application.

---

## 1. Zero Personal Data Collection & Offline-First Philosophy
- **No Personal Identifiable Information (PII)**: ScanFlow QR does not collect, track, or share personal data such as your name, email address, phone number, contacts, or location.
- **Offline & Local Storage**: All scanned QR codes, barcodes, generated codes, and history records are stored exclusively on your device's local database (Room SQLite). None of this data is transmitted to external servers without your explicit action.

---

## 2. Device Permissions Used
To provide core functionalities, ScanFlow QR requests the following permissions:
- **Camera (`android.permission.CAMERA`)**: Used strictly for real-time scanning of QR codes and barcodes using Google ML Kit on-device processing. No photos or video frames are stored, recorded, or transmitted to remote servers.
- **Photos / Media Storage (`READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE`)**: Used solely when you manually select an image from your device gallery to decode a barcode.
- **Vibration (`android.permission.VIBRATE`)**: Used to provide haptic feedback when a barcode is successfully detected.
- **Biometric (`android.permission.USE_BIOMETRIC`)**: Used locally by the Android operating system to authenticate you if you enable the optional App Lock security feature. Biometric data never leaves your device's secure hardware.

---

## 3. Third-Party Services & On-Device Processing
- **Google ML Kit Barcode Scanning**: All barcode recognition occurs 100% on your device (on-device machine learning). No barcode data or images are sent to Google cloud servers.
- **ZXing**: Used locally for rendering barcode and QR code bitmaps.
- **No Third-Party Analytics or Advertising Trackers**: The application does not integrate third-party ad networks (such as Google AdMob) or behavioral trackers.

---

## 4. External Links
If a scanned QR code contains a web URL and you choose to open it, you will be directed to your default web browser. We do not control and are not responsible for the privacy practices or content of third-party websites.

---

## 5. Data Deletion & Retention
Since your scan and creation history is stored solely on your local device:
- You have full control to delete individual history entries or use the "Clear All History" feature within the app.
- Uninstalling the application immediately removes all app-related local databases and settings from your device.

---

## 6. Children's Privacy
ScanFlow QR does not address anyone under the age of 13 knowingly, nor do we collect personal information from children.

---

## 7. Contact Us
If you have any questions or feedback regarding this Privacy Policy, you may contact us at:
- **Repository**: [https://github.com/biputsmk89-a11y/scanflow-qr](https://github.com/biputsmk89-a11y/scanflow-qr)
- **Developer Support**: biputsmk89@gmail.com
