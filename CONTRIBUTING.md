# Contributing to ScanFlow QR

Thank you for your interest in contributing to **ScanFlow QR**!

---

## 1. Code of Conduct
We are committed to providing a welcoming, inclusive, and harassment-free environment for all contributors.

---

## 2. Development Setup
1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/scanflow-qr-android.git
   cd scanflow-qr-android
   ```
2. Open the project in **Android Studio Hedgehog / Iguana / Jellyfish (2023.1.1+)**.
3. Ensure JDK 17 is configured as your Gradle JDK.
4. Run Unit Tests:
   ```bash
   ./gradlew testDebugUnitTest
   ```

---

## 3. Branching & Commit Conventions
- **Branch Naming**:
  - `feature/feature-name` (e.g. `feature/qris-parser`)
  - `bugfix/issue-description` (e.g. `bugfix/camera-rotation-leak`)
- **Conventional Commits**:
  - `feat: add custom gradient rendering to QR generator`
  - `fix: handle edge case in MeCard contact parsing`
  - `test: add unit tests for wifi encryption classification`
  - `docs: update Play Store deployment checklist`

---

## 4. Pull Request Checklist
- [ ] Code follows Clean Architecture + MVVM guidelines.
- [ ] No hardcoded strings (use `strings.xml`).
- [ ] Unit tests added or updated for new business logic / use cases.
- [ ] `./gradlew testDebugUnitTest` and `./gradlew lintDebug` pass with 0 errors.
