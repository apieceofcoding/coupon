#!/usr/bin/env bash
# 재고 100 장 쿠폰을 만들고 ID 출력. post_sellout_refresh 시나리오 사전 준비용.
# 사용: COUPON_ID=$(scripts/load/part-4/create_small_coupon.sh)

set -euo pipefail

curl -fsS -X POST http://localhost:8080/api/coupons \
  -H 'Content-Type: application/json' \
  -d '{"name":"sellout test","totalQuantity":100,"validityDays":7}' \
  | jq -r '.id'
