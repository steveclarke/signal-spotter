# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Signal Spotter is an Android app (Kotlin + Jetpack Compose) that logs where a
phone **regains cell service** while travelling through low-coverage areas, and
draws the route. It's a proof of concept, distributed as a sideloaded debug APK
via GitHub Releases (no Play Store). See `docs/SPEC.md` for the product intent.

Stack: Kotlin 2.3, Compose (BOM 2026.03), AGP 9, `minSdk 26 / target 36`,
JVM toolchain 17. No Google Play Services and no Room — location comes from raw
`LocationManager`, the map is osmdroid (no API key), persistence is a JSON file
via kotlinx.serialization.

## Build & run

```bash
./gradlew :app:assembleDebug        # -> app/build/outputs/apk/debug/app-debug.apk
```

The Android SDK is managed by Google's `android` CLI (installed at
`~/.local/bin/android`); `./gradlew` finds the SDK via `local.properties`
(`sdk.dir`). Useful CLI commands: `android sdk install <pkg>`,
`android emulator start Medium_Phone_API_36.0`, `android create`.

Install / drive a build (emulator or a wirelessly-paired device — always target
an explicit `-s <serial>` since both may be connected):

```bash
adb -s <serial> install -r <apk>
adb -s <serial> shell am start -n com.example.signalspotter/.MainActivity
adb -s <serial> exec-out run-as com.example.signalspotter cat files/trips.json   # inspect saved data
```

## Verifying changes

**There is no automated test suite.** Changes are verified by building,
installing on the emulator or device, and screenshotting. To exercise the core
behaviour without real-world conditions, drive the emulator:

```bash
adb -s <serial> emu geo fix <lon> <lat>          # set GPS (longitude first)
adb -s <serial> emu gsm voice unregistered       # drop cell service
adb -s <serial> emu gsm voice home               # regain service -> logs a spot
```

Feeding a sequence of moving `geo fix`es simulates a walk (and produces a track).

## Releasing

**Only** via the script — never hand-run the steps:

```bash
scripts/release.sh <version>     # e.g. 0.2.1
```

It syncs the version in `app/build.gradle.kts` (`versionCode` is derived from
the semver: `major*10000 + minor*100 + patch`), builds, commits `Release vX.Y.Z`,
tags, pushes, and creates the GitHub Release with the APK. Details in
`docs/RELEASING.md`.

## Architecture

The whole app is one process. The data flow is the thing to understand before
touching anything:

```
SignalLoggerService (foreground)  --writes-->  TripRepository  --StateFlow-->  Compose UI (reads)
```

- **`TripRepository`** (`data/`) is the single source of truth, **owned by
  `SignalSpotterApp`** (the `Application`, registered in the manifest) and reached
  via `(application as SignalSpotterApp).repository`. It holds all state as
  `StateFlow` (`trips`, `activeTrip`, `isLogging`, `debug`) and persists the trip
  list to `filesDir/trips.json` on every change. The service writes; the UI only
  reads. There is no DI framework — this manual singleton is the wiring.

- **`SignalLoggerService`** is a `foregroundServiceType="location"` service. On
  start it calls `repository.startTrip(...)`; while running it listens to
  telephony `ServiceState` (the no-service → in-service transition is what logs a
  signal **spot**) and continuously samples `LocationManager` GPS into the trip's
  **track** (a breadcrumb path, distinct from spots). On stop it ends the active
  trip. Start/Stop is triggered from the UI via `SignalLoggerService.start/stop`.

- **Trip model** (`data/Trip.kt`): each Start→Stop is a `Trip` containing
  `spots` (signal-regain points) **and** `track` (the path travelled). The
  repository migrates a legacy flat `spots.json` into a single trip on first run,
  and finalizes any trip left open by a killed process.

- **UI** lives in `ui/main/` (one package). `MainActivity` hosts a deliberately
  minimal two-screen navigation: an `openTripId` state switches between
  `HomeScreen` (idle/recording + trip history) and `TripDetailScreen`
  (map/list + export/rename/delete), with `BackHandler`. No navigation library.

- **`SpotsMap`** wraps an osmdroid `MapView` in `AndroidView`. **Gotcha:** set
  the camera and overlays **once** (in `DisposableEffect`, keyed on the points),
  never in the per-frame `update` block — doing the latter re-centers every
  recomposition and fights the user's panning (visible flicker). Keep
  `clipToBounds` on the map so the native view doesn't draw over sibling
  composables.

- **Export** (`export/`): `GpxExporter` builds GPX with spots as `<wpt>` and the
  track as `<trk>`; `Sharing` writes it to cache and shares via `FileProvider`.

## Conventions

- Brand color is Tailwind Green (`#16A34A` primary; mark uses `#166534` /
  `#22C55E`). Full identity and assets live in `brand/`.
- `minSdk 26` means the adaptive launcher icon (`mipmap-anydpi-v26` +
  `drawable/ic_launcher_{background,foreground}.xml`) is always used; the legacy
  `mipmap-*/ic_launcher.webp` files are dead weight.
- The package id is still the scaffold default `com.example.signalspotter`.
