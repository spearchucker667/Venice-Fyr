#!/usr/bin/env bash
set -euo pipefail

GRADLE_VERSION="9.5.0"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CACHE_DIR="${ROOT_DIR}/.gradle-bootstrap"
ZIP="${CACHE_DIR}/gradle-${GRADLE_VERSION}-bin.zip"
DIST="${CACHE_DIR}/gradle-${GRADLE_VERSION}"
URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"

if [[ -f "${ROOT_DIR}/gradle/wrapper/gradle-wrapper.jar" && -x "${ROOT_DIR}/gradlew" ]]; then
  echo "Gradle wrapper already present."
  exit 0
fi

mkdir -p "${CACHE_DIR}"
if [[ ! -x "${DIST}/bin/gradle" ]]; then
  if [[ ! -f "${ZIP}" ]]; then
    echo "Downloading Gradle ${GRADLE_VERSION} from ${URL}"
    curl --fail --location --proto '=https' --tlsv1.2 "${URL}" --output "${ZIP}"
  fi
  rm -rf "${DIST}"
  unzip -q "${ZIP}" -d "${CACHE_DIR}"
fi

"${DIST}/bin/gradle" -p "${ROOT_DIR}" wrapper \
  --gradle-version "${GRADLE_VERSION}" \
  --distribution-type bin

chmod +x "${ROOT_DIR}/gradlew"
echo "Gradle wrapper generated. Verify with: ./gradlew --version"
