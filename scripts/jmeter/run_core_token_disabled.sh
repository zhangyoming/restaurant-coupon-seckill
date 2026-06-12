#!/usr/bin/env bash
set -e
BASE_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
TS=$(date +%Y%m%d_%H%M%S)
mkdir -p "$BASE_DIR/reports"

jmeter -n   -t "$BASE_DIR/jmeter/01_core_seckill_token_disabled.jmx"   -l "$BASE_DIR/reports/core_${TS}.jtl"   -e -o "$BASE_DIR/reports/html_core_${TS}"   -Jhost=${HOST:-127.0.0.1}   -Jport=${PORT:-8081}   -JactivityId=${ACTIVITY_ID:-9001}   -Jthreads=${THREADS:-200}   -JrampUp=${RAMP_UP:-10}   -Jloops=${LOOPS:-1}   -JbaseUserId=${BASE_USER_ID:-300000}

python3 "$BASE_DIR/tools/analyze_jmeter_result.py" "$BASE_DIR/reports/core_${TS}.jtl" "$BASE_DIR/reports/core_${TS}_summary.md"
