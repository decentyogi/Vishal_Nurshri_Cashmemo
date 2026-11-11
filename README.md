# Vishal Nursery Cashmemo AI (Native Android)

Native Android port of [`Cashmemo_AI_Web`](https://github.com/decentyogi/Cashmemo_AI_Web) engineered for **100% cloud-compiled builds on GitHub Actions** (zero local Android Studio / SDK / Gradle downloads) and **seamless in-place APK updates**.

---

## Architecture & Tech Stack
- **UI:** Kotlin, Jetpack Compose, Material 3 (Vishal Nursery Maroon & Botanical Green palette)
- **Pattern:** Clean Architecture + MVVM (Unidirectional Data Flow)
- **Offline Storage:** Room Database (`CashmemoDatabase`) + Jetpack DataStore (`SettingsDataStore`)
- **Backend Sync:** Retrofit + OkHttp + Gson compatible with `Cashmemo_AI_Web`'s Flask REST API (`/api/bills`, `/api/settings`, `/api/customers`, `/api/rates`, `/api/backup`)
- **Scanner & OCR:** CameraX + Google ML Kit Text Recognition (Hindi Devanagari + English)
- **Payments & Print:** ZXing UPI QR Code generator (`upi://pay?pa=...`) + Native Android `PrintManager` HTML Cash Memo slip

---

## Zero-Local Cloud CI/CD Setup (3 Steps)

### 1. Generate Persistent Release Keystore (Zero JDK Required)
Run the lightweight Python generator (uses `keytool` if installed, or Python `cryptography` / OpenSSL locally with zero downloads):

```powershell
python scripts/generate_keystore.py
```

This creates a gitignored folder `.keystore-secrets/` containing `release-keystore.p12`, `KEYSTORE_BASE64.txt`, and `github-secrets.txt`.
**Keep `.keystore-secrets/` backed up safely** — signing every release with this exact key guarantees in-place updates on your phone forever.

### 2. Create GitHub Repo & Add 4 Repository Secrets
1. Create an empty GitHub repository named **`Vishal_Nurshri_Cashmemo`**.
2. Go to **Settings → Secrets and variables → Actions → New repository secret** and add:
   - `KEYSTORE_BASE64` — entire single-line Base64 string from `.keystore-secrets/KEYSTORE_BASE64.txt`
   - `KEYSTORE_PASSWORD` — your keystore password
   - `KEY_ALIAS` — `cashmemo-release` (or your chosen alias)
   - `KEY_PASSWORD` — your key password

### 3. Push to GitHub (Triggers Cloud APK Build & Release)
```powershell
python scripts/setup_and_push.py --github-user <YOUR_GITHUB_USERNAME>
```

GitHub Actions (`.github/workflows/build-apk.yml`) automatically runs on `ubuntu-latest`:
1. Sets up Temurin JDK 17 and Gradle 8.11.1 with caching (`gradle/actions/setup-gradle@v6`).
2. Decodes `KEYSTORE_BASE64` and verifies keystore integrity.
3. Injects `VERSION_CODE=${{ github.run_number }}` and `VERSION_NAME=1.0.${{ github.run_number }}`.
4. Compiles, minifies (`R8`), signs, verifies `zipalign` + `apksigner`, and publishes `VishalNursery-Cashmemo-v1.0.<run_number>.apk` directly under **GitHub Releases**.
