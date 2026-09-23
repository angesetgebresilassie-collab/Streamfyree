#!/usr/bin/env bash
set -euo pipefail

# Determine script location
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Run system gradle command
exec gradle --no-daemon "$@"
