#!/bin/sh
# ----------------------------------------------------------------------------
# Maven Wrapper Script for Unix / Linux / Docker
# ----------------------------------------------------------------------------

if command -v mvn >/dev/null 2>&1; then
    exec mvn "$@"
fi

if [ -n "$MAVEN_HOME" ] && [ -x "$MAVEN_HOME/bin/mvn" ]; then
    exec "$MAVEN_HOME/bin/mvn" "$@"
fi

echo "[ERROR] Maven command 'mvn' not found in PATH or MAVEN_HOME" >&2
exit 1
