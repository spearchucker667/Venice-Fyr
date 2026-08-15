#!/bin/bash
set +e
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
cd "/Users/super_user/Projects/Venice Fyr"
OUTDIR="/Users/super_user/Projects/Venice Fyr/docs/audits/venice-fyr-exhaustive-audit-2026-08-15"
LOG="$OUTDIR/validation-raw.log"

rm -f "$LOG"

echo "=== Environment ===" > "$LOG"
echo "JAVA_HOME=$JAVA_HOME" >> "$LOG"
echo "PATH=$PATH" >> "$LOG"
echo "" >> "$LOG"

echo "=== java -version ===" >> "$LOG"
java -version >> "$LOG" 2>&1
echo "EXIT_CODE_JAVA: $?" >> "$LOG"
echo "" >> "$LOG"

echo "=== ./gradlew --version ===" >> "$LOG"
./gradlew --version >> "$LOG" 2>&1
echo "EXIT_CODE_GRADLE_VERSION: $?" >> "$LOG"
echo "" >> "$LOG"

echo "=== ./gradlew test ===" >> "$LOG"
./gradlew test >> "$LOG" 2>&1
echo "EXIT_CODE_TEST: $?" >> "$LOG"
echo "" >> "$LOG"

echo "=== ./gradlew lint ===" >> "$LOG"
./gradlew lint >> "$LOG" 2>&1
echo "EXIT_CODE_LINT: $?" >> "$LOG"
echo "" >> "$LOG"

echo "=== ./gradlew :app:assembleDebug ===" >> "$LOG"
./gradlew :app:assembleDebug >> "$LOG" 2>&1
echo "EXIT_CODE_APP_DEBUG: $?" >> "$LOG"
echo "" >> "$LOG"

echo "=== ./gradlew :venice-sdk:assembleRelease ===" >> "$LOG"
./gradlew :venice-sdk:assembleRelease >> "$LOG" 2>&1
echo "EXIT_CODE_SDK_RELEASE: $?" >> "$LOG"
echo "" >> "$LOG"

echo "=== ./gradlew :app:dependencies --configuration debugRuntimeClasspath ===" >> "$LOG"
./gradlew :app:dependencies --configuration debugRuntimeClasspath >> "$LOG" 2>&1
echo "EXIT_CODE_DEPS: $?" >> "$LOG"
echo "" >> "$LOG"

echo "=== Validation run complete ===" >> "$LOG"
