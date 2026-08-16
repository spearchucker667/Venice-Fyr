#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
DEFAULT_AVD="Venice_Fyr_Agent_API_37"
AVD_NAME="${VENICE_FYR_AVD:-$DEFAULT_AVD}"
REQUIRED_API="$(sed -nE 's/^[[:space:]]*compileSdk[[:space:]]*=[[:space:]]*([0-9]+).*/\1/p' "$PROJECT_ROOT/app/build.gradle.kts" | head -n 1)"

pass() { printf 'PASS  %s\n' "$*"; }
warn() { printf 'WARN  %s\n' "$*"; }
fail() { printf 'FAIL  %s\n' "$*" >&2; exit 1; }
require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "$1 is not available on PATH"
  pass "$1: $(command -v "$1")"
}

[[ "$(uname -s)" == "Darwin" ]] || fail "This doctor currently supports macOS hosts"
ARCH="$(uname -m)"
[[ "$ARCH" == "arm64" ]] || warn "Host architecture is $ARCH; the primary Venice Fyr AVD is ARM64"
pass "macOS $(sw_vers -productVersion) ($ARCH)"

[[ -n "${ANDROID_HOME:-}" ]] || fail "ANDROID_HOME is unset; expected \$HOME/Library/Android/sdk"
[[ -n "${ANDROID_SDK_ROOT:-}" ]] || fail "ANDROID_SDK_ROOT is unset; expected the same canonical SDK root"
[[ "$ANDROID_HOME" == "$ANDROID_SDK_ROOT" ]] || fail "ANDROID_HOME and ANDROID_SDK_ROOT select different SDK roots"
[[ -d "$ANDROID_HOME" ]] || fail "Android SDK directory does not exist: $ANDROID_HOME"
pass "canonical Android SDK: $ANDROID_HOME"

for tool in adb emulator sdkmanager avdmanager java; do
  require_command "$tool"
done
[[ -x "$PROJECT_ROOT/gradlew" ]] || fail "Gradle wrapper is missing or not executable"
pass "Gradle wrapper: $PROJECT_ROOT/gradlew"

PLATFORM_DIR="$(find "$ANDROID_HOME/platforms" -mindepth 1 -maxdepth 1 -type d -name "android-$REQUIRED_API*" -print 2>/dev/null | sort -V | tail -n 1 || true)"
[[ -n "$PLATFORM_DIR" ]] || fail "required Android API $REQUIRED_API platform is not installed"
pass "required platform: ${PLATFORM_DIR##*/}"

BUILD_TOOLS="$(find "$ANDROID_HOME/build-tools" -mindepth 1 -maxdepth 1 -type d -print 2>/dev/null | sort -V | tail -n 1 || true)"
[[ -n "$BUILD_TOOLS" ]] || fail "no Android build-tools package is installed"
pass "build-tools: ${BUILD_TOOLS##*/}"

avdmanager list avd | grep -Fq "Name: $AVD_NAME" || fail "required AVD is missing: $AVD_NAME"
AVD_CONFIG="$HOME/.android/avd/$AVD_NAME.avd/config.ini"
[[ -f "$AVD_CONFIG" ]] || fail "AVD config is missing: $AVD_CONFIG"
AVD_ABI="$(sed -n 's/^abi.type=//p' "$AVD_CONFIG" | head -n 1)"
[[ "$AVD_ABI" == "arm64-v8a" ]] || fail "AVD $AVD_NAME uses ABI ${AVD_ABI:-unknown}; expected arm64-v8a"
pass "AVD: $AVD_NAME ($AVD_ABI)"

AVAILABLE_KIB="$(df -Pk "$ANDROID_HOME" | awk 'NR == 2 {print $4}')"
AVAILABLE_GIB="$((AVAILABLE_KIB / 1024 / 1024))"
(( AVAILABLE_GIB >= 10 )) || warn "only ${AVAILABLE_GIB} GiB is free on the SDK volume"
pass "available disk: ${AVAILABLE_GIB} GiB"

CONNECTED="$(adb devices | awk 'NR > 1 && $2 == "device" {print $1}')"
if [[ -z "$CONNECTED" ]]; then
  warn "no booted Android device is connected; the QA script can start $AVD_NAME"
else
  while IFS= read -r serial; do
    if [[ "$serial" == emulator-* ]]; then
      pass "connected emulator: $serial ($(adb -s "$serial" shell getprop ro.product.cpu.abi | tr -d '\r'))"
    else
      warn "physical device connected but never selected by the QA harness: $serial"
    fi
  done <<< "$CONNECTED"
fi

java -version 2>&1 | head -n 1
"$PROJECT_ROOT/gradlew" --version | sed -n '1,12p'
printf '\nAndroid emulator doctor completed successfully.\n'
