#!/usr/bin/env python3
"""
Zero-JDK / Zero-Download Android Release Keystore Generator & Base64 Encoder.

Generates a persistent PKCS12 signing keystore for seamless in-place APK updates
and outputs the 4 GitHub Repository Secrets:
  - KEYSTORE_BASE64
  - KEYSTORE_PASSWORD
  - KEY_ALIAS
  - KEY_PASSWORD

Uses `keytool` if installed; otherwise generates a Java 17-compatible PKCS12
keystore directly via Python `cryptography` or OpenSSL with zero downloads.
"""

from __future__ import annotations

import argparse
import base64
import datetime
import getpass
import os
import secrets
import shutil
import subprocess
import sys
from pathlib import Path


DEFAULT_ALIAS = "cashmemo-release"
DEFAULT_SUBJ = "CN=Vishal Nursery, OU=Mobile, O=Vishal Nursery, L=Sasaram, ST=Bihar, C=IN"


def find_openssl() -> str | None:
    on_path = shutil.which("openssl")
    if on_path:
        return on_path
    portable_openssl = Path(r"D:\AAAAA\PortableGit\usr\bin\openssl.exe")
    if portable_openssl.exists():
        return str(portable_openssl)
    return None


def generate_with_keytool(
    keytool_bin: str,
    keystore_path: Path,
    password: str,
    alias: str,
) -> None:
    cmd = [
        keytool_bin,
        "-genkeypair",
        "-v",
        "-storetype", "PKCS12",
        "-keystore", str(keystore_path),
        "-alias", alias,
        "-keyalg", "RSA",
        "-keysize", "2048",
        "-validity", "10000",
        "-storepass", password,
        "-keypass", password,
        "-dname", DEFAULT_SUBJ,
    ]
    subprocess.run(cmd, check=True)


def generate_with_python_cryptography(
    keystore_path: Path,
    password: str,
    alias: str,
) -> None:
    from cryptography import x509
    from cryptography.hazmat.primitives import hashes, serialization
    from cryptography.hazmat.primitives.asymmetric import rsa
    from cryptography.hazmat.primitives.serialization import pkcs12
    from cryptography.x509.oid import NameOID

    private_key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    subject = issuer = x509.Name(
        [
            x509.NameAttribute(NameOID.COMMON_NAME, "Vishal Nursery"),
            x509.NameAttribute(NameOID.ORGANIZATIONAL_UNIT_NAME, "Mobile"),
            x509.NameAttribute(NameOID.ORGANIZATION_NAME, "Vishal Nursery"),
            x509.NameAttribute(NameOID.LOCALITY_NAME, "Sasaram"),
            x509.NameAttribute(NameOID.STATE_OR_PROVINCE_NAME, "Bihar"),
            x509.NameAttribute(NameOID.COUNTRY_NAME, "IN"),
        ]
    )
    now = datetime.datetime.now(datetime.timezone.utc)
    cert = (
        x509.CertificateBuilder()
        .subject_name(subject)
        .issuer_name(issuer)
        .public_key(private_key.public_key())
        .serial_number(x509.random_serial_number())
        .not_valid_before(now)
        .not_valid_after(now + datetime.timedelta(days=10000))
        .add_extension(x509.BasicConstraints(ca=False, path_length=None), critical=True)
        .sign(private_key, hashes.SHA256())
    )

    p12_bytes = pkcs12.serialize_key_and_certificates(
        name=alias.encode("utf-8"),
        key=private_key,
        cert=cert,
        cas=None,
        encryption_algorithm=serialization.BestAvailableEncryption(password.encode("utf-8")),
    )
    keystore_path.write_bytes(p12_bytes)


def generate_with_openssl(
    openssl_bin: str,
    keystore_path: Path,
    password: str,
    alias: str,
) -> None:
    tmp_key = keystore_path.with_suffix(".key.tmp")
    tmp_crt = keystore_path.with_suffix(".crt.tmp")
    try:
        subprocess.run(
            [
                openssl_bin,
                "req",
                "-x509",
                "-newkey", "rsa:2048",
                "-nodes",
                "-keyout", str(tmp_key),
                "-out", str(tmp_crt),
                "-days", "10000",
                "-subj", "/CN=Vishal Nursery/OU=Mobile/O=Vishal Nursery/L=Sasaram/ST=Bihar/C=IN",
            ],
            check=True,
        )
        subprocess.run(
            [
                openssl_bin,
                "pkcs12",
                "-export",
                "-in", str(tmp_crt),
                "-inkey", str(tmp_key),
                "-out", str(keystore_path),
                "-name", alias,
                "-keypbe", "AES-256-CBC",
                "-certpbe", "AES-256-CBC",
                "-macalg", "SHA256",
                "-passout", f"pass:{password}",
            ],
            check=True,
        )
    finally:
        for temp in (tmp_key, tmp_crt):
            if temp.exists():
                temp.unlink()


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate persistent Android signing keystore & GitHub Secrets.")
    parser.add_argument("--out-dir", default=".keystore-secrets", help="Gitignored output folder (default: .keystore-secrets)")
    parser.add_argument("--alias", default=DEFAULT_ALIAS, help=f"Key alias (default: {DEFAULT_ALIAS})")
    parser.add_argument("--password", default="", help="Keystore & key password (prompted or random if omitted)")
    args = parser.parse_args()

    out_dir = Path(args.out_dir).resolve()
    out_dir.mkdir(parents=True, exist_ok=True)
    keystore_path = out_dir / "release-keystore.p12"

    if keystore_path.exists():
        print(f"[!] Keystore already exists at: {keystore_path}")
        print("    Keep this exact file forever so future APK releases update in-place!")
        raw_bytes = keystore_path.read_bytes()
        b64 = base64.b64encode(raw_bytes).decode("ascii")
        b64_file = out_dir / "KEYSTORE_BASE64.txt"
        b64_file.write_text(b64, encoding="utf-8")
        print(f"    Base64 re-exported to: {b64_file}")
        return 0

    password = args.password.strip()
    if not password:
        entered = getpass.getpass("Enter keystore password (leave blank to auto-generate strong password): ").strip()
        password = entered if entered else secrets.token_urlsafe(18)

    if len(password) < 6:
        print("Error: Password must be at least 6 characters.", file=sys.stderr)
        return 1

    alias = args.alias.strip() or DEFAULT_ALIAS

    keytool_bin = shutil.which("keytool")
    if keytool_bin:
        print(f"[*] Generating PKCS12 keystore using keytool: {keytool_bin}")
        generate_with_keytool(keytool_bin, keystore_path, password, alias)
    else:
        try:
            print("[*] `keytool` not found locally; generating Java 17-compatible PKCS12 keystore using Python cryptography...")
            generate_with_python_cryptography(keystore_path, password, alias)
        except ImportError:
            openssl_bin = find_openssl()
            if not openssl_bin:
                print("Error: Neither keytool, Python cryptography, nor openssl is available.", file=sys.stderr)
                return 1
            print(f"[*] Generating PKCS12 keystore using OpenSSL: {openssl_bin}")
            generate_with_openssl(openssl_bin, keystore_path, password, alias)

    b64 = base64.b64encode(keystore_path.read_bytes()).decode("ascii")
    (out_dir / "KEYSTORE_BASE64.txt").write_text(b64, encoding="utf-8")

    secrets_summary = (
        f"KEYSTORE_PASSWORD={password}\n"
        f"KEY_ALIAS={alias}\n"
        f"KEY_PASSWORD={password}\n"
        f"KEYSTORE_BASE64_FILE={out_dir / 'KEYSTORE_BASE64.txt'}\n"
    )
    (out_dir / "github-secrets.txt").write_text(secrets_summary, encoding="utf-8")

    print("\n=== SUCCESS: Persistent Release Keystore Created ===")
    print(f"Keystore File : {keystore_path}")
    print(f"Base64 File   : {out_dir / 'KEYSTORE_BASE64.txt'}")
    print(f"Summary File  : {out_dir / 'github-secrets.txt'}")
    print("\nAdd these 4 Repository Secrets on GitHub (Settings -> Secrets and variables -> Actions):")
    print(f"  1. KEY_ALIAS         = {alias}")
    print(f"  2. KEYSTORE_PASSWORD = {password}")
    print(f"  3. KEY_PASSWORD      = {password}")
    print(f"  4. KEYSTORE_BASE64   = (copy entire single line from {out_dir / 'KEYSTORE_BASE64.txt'})")
    print("\nIMPORTANT: Back up .keystore-secrets/ offline and NEVER commit it to Git!")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
