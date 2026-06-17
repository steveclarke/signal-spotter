# Signal Spotter

A small Android app that logs where your phone **regains cell service** while you
travel through low-coverage areas. Built for finding the spots where you can get
a text out in the Newfoundland backcountry.

Tap **Start logging** before a trip and **Stop** after. Each time the phone goes
from no-service to in-service, it records the GPS coordinate. Review the list,
export it as a `.gpx` file, and load it onto a handheld GPS.

See [`docs/SPEC.md`](docs/SPEC.md) for the design and
[`docs/PLAN.md`](docs/PLAN.md) for the build plan.

## How it works

- Logs a point on the no-service → in-service transition.
- Locates you with the phone's raw GPS (`LocationManager`) — no Google Play
  Services, works off-grid.
- Runs as a foreground service with a persistent notification so it keeps
  logging while the screen is off.
- Stores spots in a JSON file in the app's private storage.

## Installing the APK (sideload)

No Play Store needed.

1. Copy `app-debug.apk` to the phone (email it to yourself, or transfer over
   cable / cloud).
2. Tap the APK in the phone's **Files** app.
3. Android will ask to allow installs from this source — enable **"Allow from
   this source"** for the Files app (or browser), then go back.
4. You'll see a **Play Protect** warning about an unknown developer. Tap
   **Install anyway** (or "More details" → "Install anyway"). This is normal for
   apps not from the Play Store.
5. Open the app, tap **Start logging**, and grant **Location** (choose *While
   using the app* / *Allow*) and **Notifications**.

## Building from source

Requires the Android SDK. From the repo root:

```bash
./gradlew assembleDebug
```

The APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

## Status

Proof of concept. Phases 1 and 2 are built: regain-signal logging, the spot
list, GPX export/share, and an in-app OpenStreetMap (osmdroid) view with a
List/Map toggle.

**Verified** on an Android 16 (API 36) emulator: clean launch, permission flow,
and the foreground-service-location starts and runs without error (UI flips to
the logging state, location indicator active).

**Still to field-test on a real phone** (needs real cell conditions, can't be
simulated on an emulator): an actual no-service → in-service transition produces
a logged spot, the GPX share sheet, and map tiles rendering over wifi.
