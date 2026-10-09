#!/usr/bin/env bash

exec "$(dirname "$(readlink -f "$0" 2>/dev/null || echo "$0")")/ki" "$@"
