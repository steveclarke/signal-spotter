#!/usr/bin/env bash
set -euo pipefail

################################################################################
# Signal Spotter — Release Cutter
################################################################################
#
# OVERVIEW
# --------
# Cuts a new release in one deterministic, repeatable step:
#   1. Syncs the app's internal version to the release version
#   2. Builds the APK
#   3. Commits the version bump, tags it, and pushes
#   4. Creates the GitHub Release with the APK attached
#
# The Android versionCode is derived from the semver so it's always monotonic
# and reproducible: major*10000 + minor*100 + patch  (e.g. 0.2.1 -> 201).
#
# USAGE
# -----
#   scripts/release.sh <version>      # e.g. scripts/release.sh 0.2.1
#
# REQUIREMENTS
# ------------
#   - Run from the repo root, on the `main` branch, with a clean working tree
#   - `gh` authenticated (gh auth status)
#   - Android SDK reachable by ./gradlew (local.properties sdk.dir)
#
################################################################################

# Colors
green='\033[0;32m'
blue='\033[0;34m'
yellow='\033[1;33m'
red='\033[0;31m'
nc='\033[0m'

# Configuration
GRADLE_FILE="app/build.gradle.kts"
APK_BUILT="app/build/outputs/apk/debug/app-debug.apk"
MAIN_BRANCH="main"

VERSION=""
VERSION_CODE=""
TAG=""
APK_OUT=""

################################################################################
# Main Orchestration
################################################################################

main() {
  parse_arguments "$@"
  preflight
  bump_version
  build_apk
  commit_and_tag
  create_release
  log "Released ${TAG} 🎉  ($(gh release view "$TAG" --json url -q .url))"
}

################################################################################
# Helper Functions
################################################################################

log()   { echo -e "${green}==>${nc} ${1}"; }
info()  { echo -e "${blue}Info:${nc} ${1}"; }
warn()  { echo -e "${yellow}Warning:${nc} ${1}"; }
error() { echo -e "${red}Error:${nc} ${1}" >&2; exit 1; }

# Portable in-place sed (avoids GNU/BSD -i differences).
replace_in_file() {
  local file=$1 expr=$2 tmp
  tmp=$(mktemp)
  sed -E "$expr" "$file" >"$tmp" && mv "$tmp" "$file"
}

################################################################################
# Core Functions
################################################################################

parse_arguments() {
  [[ $# -eq 1 ]] || error "Usage: scripts/release.sh <version>   (e.g. 0.2.1)"
  VERSION=$1
  [[ $VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || error "Version must be semver X.Y.Z (got '$VERSION')"

  local major minor patch
  IFS=. read -r major minor patch <<<"$VERSION"
  [[ $minor -lt 100 && $patch -lt 100 ]] || error "minor/patch must each be < 100"
  VERSION_CODE=$(( major * 10000 + minor * 100 + patch ))
  TAG="v${VERSION}"
  APK_OUT="signal-spotter-${TAG}.apk"
}

preflight() {
  log "Preflight checks"
  [[ -f settings.gradle.kts && -f "$GRADLE_FILE" ]] || error "Run from the repo root"
  [[ "$(git rev-parse --abbrev-ref HEAD)" == "$MAIN_BRANCH" ]] || error "Not on '$MAIN_BRANCH' branch"
  git diff --quiet && git diff --cached --quiet || error "Working tree is dirty — commit or stash first"
  command -v gh >/dev/null || error "GitHub CLI 'gh' is not installed"
  gh auth status >/dev/null 2>&1 || error "gh is not authenticated (run: gh auth login)"
  git fetch --tags --quiet origin
  git rev-parse "$TAG" >/dev/null 2>&1 && error "Tag $TAG already exists"
  gh release view "$TAG" >/dev/null 2>&1 && error "Release $TAG already exists"
  info "Version ${VERSION}  ·  versionCode ${VERSION_CODE}  ·  tag ${TAG}"
}

bump_version() {
  log "Setting versionName=${VERSION}, versionCode=${VERSION_CODE} in ${GRADLE_FILE}"
  replace_in_file "$GRADLE_FILE" "s/versionCode = [0-9]+/versionCode = ${VERSION_CODE}/"
  replace_in_file "$GRADLE_FILE" "s/versionName = \"[^\"]*\"/versionName = \"${VERSION}\"/"
  grep -qE "versionName = \"${VERSION}\"" "$GRADLE_FILE" || error "Failed to update versionName"
}

build_apk() {
  log "Building APK"
  ./gradlew :app:assembleDebug --console=plain
  [[ -f "$APK_BUILT" ]] || error "Build did not produce $APK_BUILT"
  cp -f "$APK_BUILT" "$APK_OUT"
  info "APK: ${APK_OUT}"
}

commit_and_tag() {
  log "Committing, tagging, and pushing"
  git add "$GRADLE_FILE"
  git commit -m "Release ${TAG}"
  git tag -a "$TAG" -m "Release ${TAG}"
  git push origin "$MAIN_BRANCH"
  git push origin "$TAG"
}

create_release() {
  log "Creating GitHub release ${TAG}"
  gh release create "$TAG" "$APK_OUT" \
    --title "Signal Spotter ${TAG}" \
    --generate-notes
}

################################################################################
# Script Execution
################################################################################

main "$@"
