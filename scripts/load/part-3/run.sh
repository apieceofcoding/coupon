#!/usr/bin/env bash
# part-3 시나리오 통합 러너: 워밍업 1회 + 본 측정 1회.
# 4개 브랜치 (3-1, 3-2a, 3-2b, 3-2c) 에서 동일하게 동작한다.
#
# 사용:
#   ./scripts/load/part-3/run.sh            # 워밍업 + 본 측정 (기본)
#   ./scripts/load/part-3/run.sh --once     # 한 회차만 (워밍업 생략)
#
# 핵심: 서비스 프로세스는 띄운 채로 두고 회차 사이에 reset.sh 로 DB/Redis 만 비운다.
#       JVM JIT, HikariCP 풀, Lettuce/Kafka 컨슈머 상태가 유지돼야 2회차가 steady-state.

set -euo pipefail
cd "$(dirname "$0")/../../.."

WARMUP=true
if [[ "${1:-}" == "--once" ]]; then WARMUP=false; fi

run_once() {
  local label="$1"
  printf '\n\033[1;36m===== %s =====\033[0m\n' "$label"
  ./scripts/load/reset.sh
  local coupon_id
  coupon_id=$(./scripts/load/create_coupon.sh)
  k6 run -e COUPON_ID="$coupon_id" scripts/load/part-3/issue_burst.js
  COUPON_ID="$coupon_id" ./scripts/load/part-3/verify_burst.sh
}

if $WARMUP; then
  run_once "워밍업 1/2 (JIT, 커넥션풀, 컨슈머 그룹 조인 비용 흡수)"
  run_once "본 측정 2/2 (steady-state)"
else
  run_once "본 측정 1/1"
fi
