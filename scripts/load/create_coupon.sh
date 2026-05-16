#!/usr/bin/env bash
# 쿠폰 한 개를 만들고 ID 를 출력. 사용: COUPON_ID=$(scripts/load/create_coupon.sh)

# 명령 실패 / 미정의 변수 / 파이프 중간 실패 시 즉시 종료
set -euo pipefail

curl -fsS -X POST http://localhost:8080/api/coupons \
  -H 'Content-Type: application/json' \
  -d '{"name":"load test","totalQuantity":5000,"validityDays":7}' \
  | jq -r '.id'
