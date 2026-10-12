#!/usr/bin/env sh
set -eu
VER=9.5.1
HOME_DIR="${HOME}/.gradle/vmtrade-wrapper/gradle-${VER}"
if [ ! -x "${HOME_DIR}/bin/gradle" ]; then
  mkdir -p "${HOME}/.gradle/vmtrade-wrapper"
  TMP="${TMPDIR:-/tmp}/gradle-${VER}.zip"
  curl -fL "https://services.gradle.org/distributions/gradle-${VER}-bin.zip" -o "$TMP"
  rm -rf "${HOME}/.gradle/vmtrade-wrapper/gradle-${VER}"
  unzip -q "$TMP" -d "${HOME}/.gradle/vmtrade-wrapper"
  rm -f "$TMP"
fi
exec "${HOME_DIR}/bin/gradle" "$@"
