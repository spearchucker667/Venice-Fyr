#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
AVD_NAME="${VENICE_FYR_AVD:-Venice_Fyr_Agent_API_37}"
PACKAGE="io.github.spearchucker667.veniceforge.android"
HEADLESS=0
RUN_DEVICE_TESTS=1

usage() {
  printf 'Usage: %s [--avd NAME] [--headless] [--skip-device-tests]\n' "${0##*/}"
}

while (($#)); do
  case "$1" in
    --avd) [[ $# -ge 2 ]] || { usage >&2; exit 2; }; AVD_NAME="$2"; shift 2 ;;
    --headless) HEADLESS=1; shift ;;
    --skip-device-tests) RUN_DEVICE_TESTS=0; shift ;;
    -h|--help) usage; exit 0 ;;
    *) printf 'Unknown argument: %s\n' "$1" >&2; usage >&2; exit 2 ;;
  esac
done

fail() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }
[[ -n "${ANDROID_HOME:-}" ]] || fail "ANDROID_HOME is unset"
[[ -n "${ANDROID_SDK_ROOT:-}" ]] || fail "ANDROID_SDK_ROOT is unset"
[[ "$ANDROID_HOME" == "$ANDROID_SDK_ROOT" ]] || fail "ANDROID_HOME and ANDROID_SDK_ROOT must match"
for tool in adb emulator avdmanager; do command -v "$tool" >/dev/null 2>&1 || fail "$tool is unavailable"; done
avdmanager list avd | grep -Fq "Name: $AVD_NAME" || fail "AVD does not exist: $AVD_NAME"

STAMP="$(date '+%Y%m%d-%H%M%S')"
EVIDENCE_DIR="$PROJECT_ROOT/artifacts/android-qa/$STAMP"
mkdir -p "$EVIDENCE_DIR/test-results"
printf 'Evidence: %s\n' "$EVIDENCE_DIR"

find_target_serial() {
  local serial candidate
  while read -r candidate state; do
    [[ "$candidate" == emulator-* && "$state" == "device" ]] || continue
    serial="$candidate"
    if [[ "$(adb -s "$serial" emu avd name 2>/dev/null | tr -d '\r' | head -n 1)" == "$AVD_NAME" ]]; then
      printf '%s\n' "$serial"
      return 0
    fi
  done < <(adb devices | tail -n +2)
  return 1
}

SERIAL="$(find_target_serial || true)"
if [[ -z "$SERIAL" ]]; then
  EMULATOR_ARGS=(-avd "$AVD_NAME" -no-audio -no-boot-anim -gpu swiftshader -netdelay none -netspeed full)
  (( HEADLESS == 0 )) || EMULATOR_ARGS+=(-no-window)
  emulator "${EMULATOR_ARGS[@]}" >"$EVIDENCE_DIR/emulator.log" 2>&1 &
  printf '%s\n' "$!" >"$EVIDENCE_DIR/emulator.pid"
  for _ in $(seq 1 120); do
    SERIAL="$(find_target_serial || true)"
    [[ -n "$SERIAL" ]] && break
    sleep 2
  done
fi
[[ -n "$SERIAL" ]] || fail "AVD $AVD_NAME did not register with adb"
[[ "$SERIAL" == emulator-* ]] || fail "refusing to target non-emulator serial: $SERIAL"
printf '%s\n' "$SERIAL" >"$EVIDENCE_DIR/serial.txt"

for _ in $(seq 1 180); do
  [[ "$(adb -s "$SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]] && break
  sleep 2
done
[[ "$(adb -s "$SERIAL" shell getprop sys.boot_completed | tr -d '\r')" == "1" ]] || fail "emulator did not complete boot"
adb -s "$SERIAL" shell input keyevent 82 >/dev/null

{
  sw_vers
  uname -a
  printf 'ANDROID_HOME=%s\nANDROID_SDK_ROOT=%s\nAVD=%s\nSERIAL=%s\n' "$ANDROID_HOME" "$ANDROID_SDK_ROOT" "$AVD_NAME" "$SERIAL"
  adb version
  emulator -version 2>&1 | head -n 3
  java -version 2>&1
  "$PROJECT_ROOT/gradlew" --version
} >"$EVIDENCE_DIR/environment.txt"
git -C "$PROJECT_ROOT" status --short --branch >"$EVIDENCE_DIR/git-state.txt"
git -C "$PROJECT_ROOT" log -1 --format='%H %s' >>"$EVIDENCE_DIR/git-state.txt"
adb devices -l >"$EVIDENCE_DIR/adb-devices.txt"
{
  for prop in ro.build.version.release ro.build.version.sdk ro.product.cpu.abi ro.product.model ro.boot.qemu.avd_name; do
    printf '%s=%s\n' "$prop" "$(adb -s "$SERIAL" shell getprop "$prop" | tr -d '\r')"
  done
} >"$EVIDENCE_DIR/device-properties.txt"

export ANDROID_SERIAL="$SERIAL"
"$PROJECT_ROOT/gradlew" :app:installDebug --console=plain 2>&1 | tee "$EVIDENCE_DIR/gradle-build.log"
adb -s "$SERIAL" shell pm list packages "$PACKAGE" >"$EVIDENCE_DIR/installed-package.txt"
ACTIVITY="$(adb -s "$SERIAL" shell cmd package resolve-activity --brief "$PACKAGE" | tr -d '\r' | tail -n 1)"
[[ "$ACTIVITY" == "$PACKAGE/"* ]] || fail "could not resolve launch activity for $PACKAGE"
printf '%s\n' "$ACTIVITY" >"$EVIDENCE_DIR/resolved-activity.txt"

adb -s "$SERIAL" logcat -c
adb -s "$SERIAL" shell am force-stop "$PACKAGE"
adb -s "$SERIAL" shell am start -W -n "$ACTIVITY" >"$EVIDENCE_DIR/launch.txt"
FOREGROUND=0
for _ in $(seq 1 20); do
  adb -s "$SERIAL" shell dumpsys activity activities >"$EVIDENCE_DIR/activities.txt"
  if grep -E "topResumedActivity=.*$PACKAGE" "$EVIDENCE_DIR/activities.txt" >/dev/null; then
    FOREGROUND=1
    break
  fi
  sleep 1
done
(( FOREGROUND == 1 )) || fail "launched activity did not become the foreground activity"

capture_ui() {
  local label="$1"
  local remote="/sdcard/venice-fyr-$label.xml"
  local captured=0
  adb -s "$SERIAL" shell rm -f "$remote"
  for _ in 1 2 3; do
    adb -s "$SERIAL" shell uiautomator dump "$remote" >/dev/null 2>&1 || true
    if adb -s "$SERIAL" shell test -s "$remote"; then
      adb -s "$SERIAL" pull "$remote" "$EVIDENCE_DIR/ui-$label.xml" >/dev/null
      if python3 - "$EVIDENCE_DIR/ui-$label.xml" <<'PY'
import sys, xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
raise SystemExit(0 if root.tag == 'hierarchy' and list(root) else 1)
PY
      then
        captured=1
        break
      fi
    fi
    sleep 1
  done
  (( captured == 1 )) || fail "could not capture a fresh, non-empty $label UI hierarchy"
  adb -s "$SERIAL" exec-out screencap -p >"$EVIDENCE_DIR/screen-$label.png"
}
capture_ui initial

# Exercise the navigation drawer using the accessibility-visible Menu control.
MENU_BOUNDS="$(python3 - "$EVIDENCE_DIR/ui-initial.xml" <<'PY'
import re, sys, xml.etree.ElementTree as ET
for node in ET.parse(sys.argv[1]).iter('node'):
    if node.attrib.get('text') == 'Menu':
        match = re.fullmatch(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', node.attrib.get('bounds', ''))
        if match:
            x1, y1, x2, y2 = map(int, match.groups())
            print((x1 + x2) // 2, (y1 + y2) // 2)
            break
PY
)"
if [[ -n "$MENU_BOUNDS" ]]; then
  read -r MENU_X MENU_Y <<< "$MENU_BOUNDS"
  adb -s "$SERIAL" shell input tap "$MENU_X" "$MENU_Y"
  sleep 1
  adb -s "$SERIAL" shell input swipe 600 2050 600 550 800
  sleep 1
  adb -s "$SERIAL" shell input swipe 600 2050 600 900 700
  sleep 1
fi
capture_ui final
NAVIGATION_STATUS="BLOCKED (Menu control not found)"
if [[ -n "$MENU_BOUNDS" ]] && grep -Fq 'text="Config"' "$EVIDENCE_DIR/ui-final.xml"; then
  NAVIGATION_STATUS="VERIFIED"
fi

adb -s "$SERIAL" logcat -d -v threadtime >"$EVIDENCE_DIR/logcat.txt"
adb -s "$SERIAL" logcat -d -b crash -v threadtime >"$EVIDENCE_DIR/crashes.txt"
grep -Ei 'StrictMode|policy violation' "$EVIDENCE_DIR/logcat.txt" >"$EVIDENCE_DIR/strictmode.txt" || true

TEST_STATUS="NOT RUN (no androidTest sources)"
if (( RUN_DEVICE_TESTS == 1 )) && find "$PROJECT_ROOT/app/src" -path '*/androidTest/*' -type f -print -quit | grep -q .; then
  if "$PROJECT_ROOT/gradlew" :app:connectedDebugAndroidTest --console=plain 2>&1 | tee "$EVIDENCE_DIR/gradle-tests.log"; then
    TEST_STATUS="VERIFIED"
  else
    TEST_STATUS="FAILED"
  fi
  find "$PROJECT_ROOT/app/build" -path '*androidTest-results*' -type f -exec cp {} "$EVIDENCE_DIR/test-results/" \; 2>/dev/null || true
else
  printf '%s\n' "$TEST_STATUS" >"$EVIDENCE_DIR/gradle-tests.log"
fi

if [[ "$TEST_STATUS" == "VERIFIED" ]]; then
  "$PROJECT_ROOT/gradlew" :app:installDebug --console=plain >>"$EVIDENCE_DIR/gradle-build.log" 2>&1
  adb -s "$SERIAL" shell am start -W -n "$ACTIVITY" >"$EVIDENCE_DIR/post-test-launch.txt"
  POST_TEST_FOREGROUND=0
  for _ in $(seq 1 20); do
    if adb -s "$SERIAL" shell dumpsys activity activities | grep -E "topResumedActivity=.*$PACKAGE" >/dev/null; then
      POST_TEST_FOREGROUND=1
      break
    fi
    sleep 1
  done
  (( POST_TEST_FOREGROUND == 1 )) || fail "app did not return to foreground after device tests"
fi

CRASH_LINES="$(wc -l <"$EVIDENCE_DIR/crashes.txt" | tr -d ' ')"
cat >"$EVIDENCE_DIR/summary.md" <<EOF
# Venice Fyr Android Emulator QA

- AVD: \`$AVD_NAME\`
- Serial: \`$SERIAL\`
- Activity: \`$ACTIVITY\`
- Build/install: VERIFIED
- Launch/foreground: VERIFIED
- UI hierarchy: VERIFIED
- Screenshots: VERIFIED
- Navigation smoke: $NAVIGATION_STATUS
- Device tests: $TEST_STATUS
- Crash-buffer lines: $CRASH_LINES
EOF

printf '\nQA completed: %s\n' "$EVIDENCE_DIR"
printf 'Device tests: %s\n' "$TEST_STATUS"
printf 'Crash-buffer lines: %s\n' "$CRASH_LINES"
[[ "$TEST_STATUS" != "FAILED" ]] || exit 1
