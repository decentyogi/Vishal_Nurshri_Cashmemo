#!/usr/bin/env python3
"""
Ultra-Low Bandwidth Repo Scaffolder & GitHub Push Script.

- Scaffolds minimal required Android folder structure & build/CI files if missing:
  settings.gradle.kts, build.gradle.kts, app/build.gradle.kts,
  app/src/main/AndroidManifest.xml, and .github/workflows/build-apk.yml.
- Never overwrites existing full Kotlin source files unless --force is passed.
- Auto-discovers `git` (including PortableGit on Windows) and pushes to GitHub.
"""

from __future__ import annotations

import argparse
import shutil
import subprocess
from pathlib import Path


SETTINGS_GRADLE = """pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "Vishal_Nurshri_Cashmemo"
include(":app")
"""

ROOT_BUILD_GRADLE = """plugins {
    id("com.android.application") version "8.8.2" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false
}
"""

APP_BUILD_GRADLE = """plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val ciVersionCode: Int = System.getenv("VERSION_CODE")?.toIntOrNull()?.coerceAtLeast(1) ?: 1
val ciVersionName: String = System.getenv("VERSION_NAME")?.takeIf { it.isNotBlank() } ?: "1.0.$ciVersionCode"

android {
    namespace = "com.vishalnursery.cashmemo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.vishalnursery.cashmemo"
        minSdk = 24
        targetSdk = 35
        versionCode = ciVersionCode
        versionName = ciVersionName
    }

    signingConfigs {
        create("release") {
            val ksFile = file(System.getenv("KEYSTORE_PATH") ?: "release-keystore.p12")
            val ksPassword = System.getenv("KEYSTORE_PASSWORD")
            val ksAlias = System.getenv("KEY_ALIAS")
            val kPassword = System.getenv("KEY_PASSWORD")
            if (ksFile.exists() && !ksPassword.isNullOrBlank() && !ksAlias.isNullOrBlank() && !kPassword.isNullOrBlank()) {
                storeFile = ksFile
                storePassword = ksPassword
                keyAlias = ksAlias
                keyPassword = kPassword
            }
        }
    }

    buildTypes {
        release {
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile?.exists() == true) {
                signingConfig = releaseSigning
            }
            isMinifyEnabled = true
            isShrinkResources = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
}
"""

MANIFEST_XML = """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera.any" android:required="false" />
    <application
        android:label="Vishal Nursery Cashmemo"
        android:supportsRtl="true"
        android:usesCleartextTraffic="true">
        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
"""

WORKFLOW_YML = """name: Build & Release Signed Android APK

on:
  push:
    branches: [ main ]
  workflow_dispatch:

permissions:
  contents: write

jobs:
  build-release-apk:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
      - name: Decode persistent signing keystore
        env:
          KEYSTORE_BASE64: ${{ secrets.KEYSTORE_BASE64 }}
        run: |
          printf '%s' "$KEYSTORE_BASE64" | tr -d '\\r\\n ' | base64 --decode > "${{ github.workspace }}/app/release-keystore.p12"
      - uses: gradle/actions/setup-gradle@v6
        with:
          gradle-version: '8.11.1'
      - name: Compile & Sign Release APK
        env:
          VERSION_CODE: ${{ github.run_number }}
          VERSION_NAME: 1.0.${{ github.run_number }}
          KEYSTORE_PATH: ${{ github.workspace }}/app/release-keystore.p12
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        run: gradle assembleRelease --no-daemon --stacktrace
      - name: Clean up decoded keystore
        if: always()
        run: rm -f "${{ github.workspace }}/app/release-keystore.p12"
      - uses: softprops/action-gh-release@v3
        with:
          tag_name: v1.0.${{ github.run_number }}
          files: app/build/outputs/apk/release/app-release.apk
          fail_on_unmatched_files: true
          generate_release_notes: true
"""


def find_git() -> str:
    on_path = shutil.which("git")
    if on_path:
        return on_path
    candidates = [
        Path(r"D:\AAAAA\PortableGit\cmd\git.exe"),
        Path(r"C:\Program Files\Git\cmd\git.exe"),
        Path(r"C:\Program Files (x86)\Git\cmd\git.exe"),
    ]
    for candidate in candidates:
        if candidate.exists():
            return str(candidate)
    raise FileNotFoundError("git executable not found on PATH or standard install locations.")


def ensure_file(path: Path, content: str, force: bool = False) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if force or not path.exists():
        path.write_text(content, encoding="utf-8")
        print(f"[+] Wrote {path}")


def scaffold_minimal(root: Path, force: bool = False) -> None:
    for rel in [
        "app/src/main/java/com/vishalnursery/cashmemo",
        "app/src/main/res/values",
        ".github/workflows",
    ]:
        (root / rel).mkdir(parents=True, exist_ok=True)

    ensure_file(root / "settings.gradle.kts", SETTINGS_GRADLE, force=force)
    ensure_file(root / "build.gradle.kts", ROOT_BUILD_GRADLE, force=force)
    ensure_file(root / "app/build.gradle.kts", APP_BUILD_GRADLE, force=force)
    ensure_file(root / "app/src/main/AndroidManifest.xml", MANIFEST_XML, force=force)
    ensure_file(root / ".github/workflows/build-apk.yml", WORKFLOW_YML, force=force)


def run_git(git_bin: str, args: list[str], cwd: Path, check: bool = True) -> subprocess.CompletedProcess:
    print(f"$ git {' '.join(args)}")
    return subprocess.run([git_bin, *args], cwd=cwd, check=check)


def main() -> int:
    parser = argparse.ArgumentParser(description="Initialize & push Vishal_Nurshri_Cashmemo to GitHub.")
    parser.add_argument("--github-user", default="decentyogi", help="GitHub username (default: decentyogi)")
    parser.add_argument("--repo", default="Vishal_Nurshri_Cashmemo", help="GitHub repo name")
    parser.add_argument("--force", action="store_true", help="Overwrite minimal scaffold files")
    parser.add_argument("--no-push", action="store_true", help="Commit locally without pushing")
    args = parser.parse_args()

    # If run from scripts/, target parent project root; otherwise target cwd
    script_parent = Path(__file__).resolve().parent
    root = script_parent.parent if script_parent.name == "scripts" else Path.cwd()
    scaffold_minimal(root, force=args.force)

    git_bin = find_git()
    remote_url = f"https://github.com/{args.github_user}/{args.repo}.git"

    if not (root / ".git").exists():
        run_git(git_bin, ["init", "-b", "main"], cwd=root)
    else:
        run_git(git_bin, ["branch", "-M", "main"], cwd=root)

    run_git(git_bin, ["add", "."], cwd=root)
    status = subprocess.run([git_bin, "status", "--porcelain"], cwd=root, capture_output=True, text=True, check=True)
    if status.stdout.strip():
        run_git(git_bin, ["commit", "-m", "Initial cloud-first Android Cashmemo AI setup"], cwd=root)
    else:
        print("[*] Working tree clean; nothing new to commit.")

    remotes = subprocess.run([git_bin, "remote"], cwd=root, capture_output=True, text=True, check=True).stdout.splitlines()
    if "origin" in remotes:
        run_git(git_bin, ["remote", "set-url", "origin", remote_url], cwd=root)
    else:
        run_git(git_bin, ["remote", "add", "origin", remote_url], cwd=root)

    if args.no_push:
        print(f"[*] Remote configured ({remote_url}). Skipped push (--no-push).")
        return 0

    print(f"[*] Pushing main branch to {remote_url} ...")
    run_git(git_bin, ["push", "-u", "origin", "main"], cwd=root)
    print("[✓] Push complete! GitHub Actions will now compile, sign, and publish the APK under Releases.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
