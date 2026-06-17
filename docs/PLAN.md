# Signal Spotter — Implementation Plan

Built on the `android create empty-activity` Compose scaffold. Navigation3 is
stripped out (single screen).

## Phase 1 — Core (logging + list + GPX export)

1. **Data model** — `data/LoggedSpot.kt`: `@Serializable data class` with
   timestamp, lat, lon, accuracy, carrier.
2. **Repository** — `data/SpotRepository.kt`: holds `StateFlow<List<LoggedSpot>>`
   and `StateFlow<Boolean>` (isLogging), persists the list to
   `filesDir/spots.json`. `add`, `clear`, `setLogging`.
3. **Application** — `SignalSpotterApp.kt`: owns the single `SpotRepository`.
   Registered in the manifest.
4. **Foreground service** — `service/SignalLoggerService.kt`:
   - `startForeground` with a low-importance ongoing notification.
   - Continuous `LocationManager` GPS (+ network) updates → `lastLocation`.
   - `TelephonyCallback.ServiceStateListener` on API 31+, `PhoneStateListener`
     fallback below. On no-service → in-service transition, log `lastLocation`
     with the current carrier name.
   - `start`/`stop` companion helpers; flips `repository.setLogging`.
5. **GPX export** — `export/GpxExporter.kt` builds a GPX 1.1 waypoint document;
   `export/Sharing.kt` writes it to cache and fires an `ACTION_SEND` chooser via
   FileProvider.
6. **UI** — `ui/main/MainScreen.kt` + `ui/main/MainViewModel.kt`
   (`AndroidViewModel`): Start/Stop button, status line, spot count, Export GPX
   and Clear actions, and a newest-first list. Runtime permission flow
   (location + notifications) gates Start.
7. **Manifest** — permissions, `.SignalSpotterApp`, the service with
   `foregroundServiceType="location"`, and the FileProvider + `file_paths.xml`.
8. **Cleanup** — delete scaffold's `Navigation.kt`, `NavigationKeys.kt`,
   `data/DataRepository.kt`, and the two stale tests. Point `MainActivity` at
   `MainScreen`.
9. **Build** `./gradlew assembleDebug`, fix until the APK is produced.

## Phase 2 — Map

10. Add `org.osmdroid:osmdroid-android`. Configure osmdroid (user agent +
    internal tile cache) in the Application. Add an `AndroidView`-wrapped
    `MapView` showing the spots as markers, toggled from the main screen.

## Out of scope for now

Boot auto-start, cloud sync, offline tiles, in-app SMS composing.
