#!/usr/bin/env python3
"""Build the standalone LSPosed APK using a local Android SDK."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import zipfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent
API_SHA256 = "f48c635f1c7469fdec0e00ad2ea0b7a6b2f5b55065784a35b7ca3a84615e8e25"


def run(*args):
    subprocess.run([str(arg) for arg in args], check=True)


parser = argparse.ArgumentParser()
parser.add_argument("--sdk", type=Path, default=os.environ.get("ANDROID_SDK_ROOT") or os.environ.get("ANDROID_HOME"))
parser.add_argument("--build-tools", default="35.0.0")
parser.add_argument("--keystore", type=Path, help="Existing PKCS12/JKS signing key; otherwise a local debug key is created")
parser.add_argument("--key-alias", default="categories", help="Signing-key alias (default: categories)")
parser.add_argument("--ks-pass", default="pass:android", help="apksigner keystore password argument (default: pass:android)")
options = parser.parse_args()
if options.sdk is None:
    parser.error("Set ANDROID_SDK_ROOT / ANDROID_HOME or pass --sdk PATH")
tools = options.sdk / "build-tools" / options.build_tools
android = options.sdk / "platforms/android-29/android.jar"
api = ROOT / "deps/xposed-api-82.jar"
if not api.exists() or hashlib.sha256(api.read_bytes()).hexdigest() != API_SHA256:
    raise SystemExit("Missing/mismatched Xposed API jar. Run python3 fetch-deps.py first.")
for path in [android, tools / "aapt", tools / "d8", tools / "apksigner", tools / "zipalign"]:
    if not path.exists():
        raise SystemExit(f"Missing Android SDK tool: {path}")

build = ROOT / "build"
classes = build / "classes"
dist = ROOT / "dist"
classes.mkdir(parents=True, exist_ok=True)
dist.mkdir(exist_ok=True)
for stale in classes.rglob("*.class"):
    stale.unlink()

manifest = ET.parse(ROOT / "AndroidManifest.xml").getroot()
package = manifest.get("package")
version = manifest.get("{http://schemas.android.com/apk/res/android}versionName")
run("javac", "--release", "8", "-classpath", os.pathsep.join([str(android), str(api)]),
    "-d", classes, *sorted((ROOT / "src").rglob("*.java")))
run(tools / "d8", "--min-api", "29", "--lib", android, "--classpath", api,
    "--output", build, *sorted(classes.rglob("*.class")))

unsigned = build / "unsigned.apk"
run(tools / "aapt", "package", "-f", "-M", ROOT / "AndroidManifest.xml", "-S", ROOT / "res",
    "-A", ROOT / "assets", "-I", android, "-F", unsigned)
with zipfile.ZipFile(unsigned, "a", compression=zipfile.ZIP_DEFLATED) as apk:
    apk.write(build / "classes.dex", "classes.dex")
aligned = build / "aligned.apk"
run(tools / "zipalign", "-f", "-p", "4", unsigned, aligned)

key = options.keystore or build / "categories-debug.p12"
if not key.exists():
    if options.keystore:
        raise SystemExit("Specified signing key does not exist.")
    run("keytool", "-genkeypair", "-keystore", key, "-storetype", "PKCS12",
        "-storepass", "android", "-keypass", "android", "-alias", "categories",
        "-keyalg", "RSA", "-keysize", "2048", "-validity", "3650",
        "-dname", "CN=Library Categories Debug")
target = dist / f"pico-librarycategories-{version}-debug.apk"
run(tools / "apksigner", "sign", "--ks", key, "--ks-pass", options.ks_pass,
    "--ks-key-alias", options.key_alias,
    "--out", target, aligned)
run(tools / "apksigner", "verify", "--verbose", "--min-sdk-version", "29", target)
run(tools / "zipalign", "-c", "-p", "4", target)
with zipfile.ZipFile(target) as apk:
    entry = (ROOT / "assets/xposed_init").read_text().strip()
    assert apk.read("assets/xposed_init").decode().strip() == entry
    assert ("L" + entry.replace(".", "/") + ";").encode() in apk.read("classes.dex")
    assert ("L" + package.replace(".", "/") + "/MainActivity;").encode() in apk.read("classes.dex")

sources = sorted((ROOT / "src").rglob("*.java"))
tracked_inputs = sources + [ROOT / "AndroidManifest.xml", ROOT / "assets/xposed_init"] + sorted((ROOT / "res").rglob("*.xml"))
metadata = {"apk": target.name, "package": package, "version": version,
            "sha256": hashlib.sha256(target.read_bytes()).hexdigest(),
            "compile_sdk": 29, "min_sdk": 29, "build_tools": options.build_tools,
            "xposed_compile_api_sha256": API_SHA256, "runtime_tested": False,
            "source_sha256": {str(path.relative_to(ROOT)): hashlib.sha256(path.read_bytes()).hexdigest()
                              for path in tracked_inputs}}
(dist / "build-info.json").write_text(json.dumps(metadata, indent=2) + "\n")
print(target)
