#!/bin/sh

# RuqaiyaPro Gradle wrapper script for Linux / Termux / CI
# Target: Infinix HOT 12 Play X6816C

DIRNAME=`dirname "$0"`
APP_BASE_NAME=`basename "$0"`
APP_HOME=`cd "$DIRNAME" && pwd`

exec "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@" 2>/dev/null || {
  if command -v gradle >/dev/null 2>&1; then
    gradle "$@"
  else
    echo "Gradle wrapper or local gradle required. Run: pkg install openjdk-17 gradle in Termux"
    exit 1
  fi
}
