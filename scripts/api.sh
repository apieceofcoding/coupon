#!/usr/bin/env bash
set -e
BASE="${BASE_URL:-http://localhost:8080}"

COUPON=$(curl -sS -X POST "$BASE/api/coupons" \
  -H 'Content-Type: application/json' \
  -d '{"name":"여름 할인","totalQuantity":100,"validityDays":7}' | jq -r '.id')

ISSUANCE=$(curl -sS -X POST "$BASE/api/coupons/$COUPON/issue" -H 'X-User-Id: 1' | jq -r '.id')

curl -sS -X POST "$BASE/api/issuances/$ISSUANCE/use" -H 'X-User-Id: 1'
curl -sS "$BASE/api/users/me/issuances" -H 'X-User-Id: 1'
