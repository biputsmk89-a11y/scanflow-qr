# Master Test Plan & Quality Assurance Matrix — ScanFlow QR

Comprehensive test plan covering Functional, Permission, Device Compatibility, Lifecycle, Security, and Offline-first scenarios for **ScanFlow QR (v1.0.0)**.

---

## 1. Functional Test Matrix

| ID | Module / Feature | Scenario | Expected Result | Status |
| :--- | :--- | :--- | :--- | :--- |
| **TC-01** | Scanner | Point camera at valid QR URL | Fast detection, sound/vibrate, parses URL, opens Result screen | ✅ PASS |
| **TC-02** | Scanner | Point camera at EAN-13 / Code 128 barcode | Correctly classifies as BARCODE and displays symbology format | ✅ PASS |
| **TC-03** | Scanner | Toggle flashlight in dark room | Torch turns on/off smoothly without stuttering viewfinder | ✅ PASS |
| **TC-04** | Scanner | Tap switch camera button | Switches from Back to Front camera lens seamlessly | ✅ PASS |
| **TC-05** | Scanner | Scan image from Gallery | Opens Photo Picker, detects barcode inside photo, navigates to Result | ✅ PASS |
| **TC-06** | Result & Security | Scan HTTPS link vs HTTP link | HTTPS shows green "Verified HTTPS"; HTTP shows amber warning banner | ✅ PASS |
| **TC-07** | Result & Actions | Tap "Open Website" / "Dial Phone" / "Send SMS" | Safely launches appropriate Android system intent without auto-executing | ✅ PASS |
| **TC-08** | Generator | Fill Wi-Fi form (SSID, Pass, WPA) & Generate | Generates standard `WIFI:...` format QR preview bitmap | ✅ PASS |
| **TC-09** | Generator | Fill Contact form (vCard) & Generate | Generates valid vCard 3.0 compatible QR code | ✅ PASS |
| **TC-10** | Customizer | Change foreground color to Cyan & pattern to Dots | Bitmap re-renders in real-time with updated dot styling | ✅ PASS |
| **TC-11** | Export & Share | Tap "Save to Gallery (PNG)" | Image saved to `Pictures/ScanFlowQR` and indexed in MediaStore | ✅ PASS |
| **TC-12** | History | Search past scans by query keyword | Live filtering updates Room Flow within <16ms | ✅ PASS |
| **TC-13** | History | Batch select 3 items and tap Delete | Selected 3 entities removed permanently from SQLite database | ✅ PASS |
| **TC-14** | Favorites | Tap heart icon on scan result | State toggles, reflected immediately in Favorites tab | ✅ PASS |
| **TC-15** | App Lock | Enable App Lock & Biometrics in Settings | Prompt displays upon app launch; unlocks on fingerprint success | ✅ PASS |

---

## 2. Permission Test Matrix

| ID | Permission | Test Action | Expected Behavior |
| :--- | :--- | :--- | :--- |
| **PT-01** | Camera | User clicks "Allow" | Viewfinder opens immediately; starts scanning |
| **PT-02** | Camera | User clicks "Deny" | Clean Empty State with "Grant Permission" CTA appears |
| **PT-03** | Camera | Revoke permission via Android Settings | App re-prompts gracefully without crashing |
| **PT-04** | Storage / Photos | Photo Picker launch | User selects photo; no broad storage permission requested on API 33+ |

---

## 3. Lifecycle & Device Compatibility

| ID | Category | Scenario | Expected Behavior |
| :--- | :--- | :--- | :--- |
| **LT-01** | Rotation | Rotate screen 90° during scanning | Viewfinder adjusts orientation; camera preview continues seamlessly |
| **LT-02** | Background | Home button pressed, app resumed | Camera session paused on pause, resumed on resume without memory leak |
| **LT-03** | Screen Size | Tested on 360dp width screen | Zero text truncation, zero button overflow, touch targets >= 48dp |
| **LT-04** | Theme | Switch between Dark and Light mode | Colors adapt dynamically following `ScanFlowQRTheme` tokens |
| **LT-05** | Offline | Airplane mode enabled | Scanning, QR generating, saving, searching, and history work 100% |
