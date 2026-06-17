# Signal Spotter — Spec

## The problem

Dike travels off-road in the Newfoundland woods, where cell signal is very
spotty. He'll compose a text, hit send, and somewhere along the drive the phone
briefly catches a signal and the message goes out — but he never knows *where*
that spot was. He wants a record of the places he can reliably get a signal out,
so on future trips he can drive to a known coordinate when he needs to reach
someone.

## What it does

A small Android app that, while running, **logs a GPS coordinate every time the
phone regains cell service** (transitions from no-service to in-service). Over a
few trips this builds a map of "I can get a text out here" spots.

### Why "regains signal" and not "where the text sent"

Precisely detecting where a queued SMS *actually sends* would require Dike to
compose his texts inside this app — Android does not reliably tell a third-party
app when the stock Messaging app sends. Logging the spot where the phone regains
service captures the same practical information (where coverage exists) without
changing how he texts. This is the right trade-off for a proof of concept.

## Core behaviour

- **Trigger:** A logged point is created on the no-service → in-service
  transition. The first state reading after start is treated as a baseline (no
  log) so we only record genuine regains.
- **Location:** Raw GNSS via Android's `LocationManager` (GPS provider, with
  network provider as backup). No Google Play Services dependency — important for
  an off-grid device and for a clean sideloaded APK. While logging is active the
  service tracks location continuously, so a fresh fix is always on hand the
  instant signal returns.
- **Captured per spot:** timestamp, latitude, longitude, fix accuracy (m), and
  the carrier name reported at that moment.
- **Run mode:** Manual. Dike taps **Start logging** before a trip and **Stop**
  after. A foreground service with a persistent notification keeps it running
  while the screen is off and the app is backgrounded during the drive.

## Output

- **In-app list** of logged spots (newest first): local time, coordinates,
  accuracy, carrier.
- **GPX export / share** — produces a standard `.gpx` waypoint file Dike can
  share to himself and load onto his dedicated handheld GPS.
- **Map** (phase 2) — an OpenStreetMap view (osmdroid, no API key) showing the
  logged spots as pins.

## Permissions

- Location (fine + coarse) — runtime grant, required.
- Notifications (Android 13+) — runtime grant, for the foreground-service
  notification.
- Foreground service + foreground-service-location — manifest only.
- Internet — for map tiles (phase 2).

Deliberately **not** requesting `READ_PHONE_STATE`: the coarse in/out-of-service
state arrives through `TelephonyCallback.ServiceStateListener` without it, which
avoids the alarming "make and manage phone calls" prompt.

## Non-goals (PoC)

- No always-on/boot auto-start, no cloud sync, no accounts.
- No precise "where did my SMS send" capture.
- No offline map tiles (the map is for reviewing at home on wifi).

## Delivery

Debug APK, sideloaded. Dike enables "install unknown apps" for his Files app or
browser, taps the APK, dismisses the Play Protect "unknown developer" notice,
and installs. No Play Store or developer account needed.

## Stack

Kotlin, Jetpack Compose (Material 3), single screen. minSdk 26, targetSdk 36.
Persistence is a JSON file via kotlinx.serialization. No Room, no Play Services.
