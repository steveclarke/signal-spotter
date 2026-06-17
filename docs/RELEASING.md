# Releasing

Cutting a release is a single command:

```bash
scripts/release.sh <version>      # e.g. scripts/release.sh 0.2.1
```

That script is the only supported way to release — don't hand-run the steps.

## What it does

1. **Syncs the internal version** in `app/build.gradle.kts`:
   - `versionName` = the version you pass (e.g. `0.2.1`)
   - `versionCode` = derived deterministically from the semver as
     `major*10000 + minor*100 + patch` (so `0.2.1` → `201`), always monotonic.
2. **Builds** the debug APK.
3. **Commits** the version bump as `Release vX.Y.Z`, **tags** `vX.Y.Z`, and
   **pushes** the commit and tag.
4. **Creates the GitHub Release** `vX.Y.Z` with the APK attached
   (`signal-spotter-vX.Y.Z.apk`) and auto-generated notes.

## Preconditions (the script checks these and stops if unmet)

- Run from the repo root, on `main`, with a clean working tree.
- `gh` installed and authenticated (`gh auth status`).
- Android SDK reachable by `./gradlew` (via `local.properties` `sdk.dir`).
- The tag / release doesn't already exist.

## Versioning

Use semver `MAJOR.MINOR.PATCH`. `minor` and `patch` must each be `< 100`
(keeps `versionCode` monotonic). Bump:

- **patch** for fixes (`0.2.0` → `0.2.1`)
- **minor** for features (`0.2.1` → `0.3.0`)
- **major** for big/breaking changes (`0.x` → `1.0.0`)

## Notes

- The APK is a **debug** build (fine for sideloading). A signed **release**
  build would be needed for the Play Store.
- Release notes are auto-generated from commits since the last tag; edit the
  release on GitHub afterward if you want to polish them.
