# PICO Library Categories

An LSPosed module that adds custom categories, ordering, visibility controls, and multi-category app assignments to the PICO Library sidebar (`com.pvr.appmanager`).

Create and edit categories from **Manage categories…** in the sidebar. Use an app’s **Categories…** menu action to assign it to one or more categories, or use **Apps…** in the manager for bulk assignment. Category membership follows package names, so renaming or reordering a category keeps its apps assigned. Removing a category removes that category’s assignments; it does not uninstall apps.

## Install

Download the APK from this repository’s **GitHub Releases** and install it on the headset. In LSPosed, enable **PICO Library Categories** for **Library** (`com.pvr.appmanager`) and for the module app itself (`research.pico.librarycategories`), then restart Library or reboot the headset. No SystemExt or Android framework scope is needed.

Requires a rooted headset with LSPosed. The module stores category data in Library’s private app storage. Clearing Library data removes the categories. Disabling or uninstalling the module restores the stock Library interface.

## Compatibility

Confirmed on PICO 4 / Phoenix OS 5.13.7 with Library 2.9.5 (versionCode 200905100). Static inspection found PICO 4 Ultra / Sparrow OS 5.14.4 with AppManager versionCode 200905142 incompatible: sidebar, filter, and menu contracts changed. Other OS and Library versions are unverified.

## Build

Use JDK 17 or newer and install Android SDK platform 29 and Build Tools 35.0.0. Set `ANDROID_SDK_ROOT` (or `ANDROID_HOME`), or pass `--sdk PATH` to the builder. The Xposed API 82 jar is compile-only and fetched with a pinned SHA-256 check.

```sh
python3 fetch-deps.py
python3 build.py
```

The signed APK and build metadata are written under ignored `dist/`. By default the builder creates a local debug key under ignored `build/`; pass `--keystore PATH`, `--key-alias NAME`, and `--ks-pass VALUE` to sign with an existing key. Keep using the same key for updates that must install over an existing build.

Run the portable host tests with:

```sh
python3 tests/run.py
```

The tests cover category identity, app membership, ordering, and drag reorder math. Firmware contract tests and proprietary firmware evidence are not part of this public source package.
