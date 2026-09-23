#!/usr/bin/env bash
set -euo pipefail

# Determine script location
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$DIR/.." && pwd )"

# Ensure gradlew executable is used if present, otherwise system gradle
if [ -f "$PROJECT_ROOT/gradlew" ]; then
    EXEC="$PROJECT_ROOT/gradlew"
else
    EXEC="gradle"
fi

exec "$EXEC" --no-daemon "$@"
