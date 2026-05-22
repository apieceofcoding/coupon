#!/usr/bin/env bash
# 재고 100 장 쿠폰을 100 명에게 발급시켜 매진 상태로 만든다.
# 사용: COUPON_ID=<id> scripts/load/part-4/sell_out.sh

set -euo pipefail
COUPON_ID="${COUPON_ID:?COUPON_ID 환경변수 필요}"

# 발급 컨트롤러는 user_id 헤더로 받기 때문에 1..100 까지 순차 호출.
# Worker 가 issuance 행을 INSERT 하는 데 시간이 좀 걸리므로 끝나면 잠시 sleep.
for uid in $(seq 1 100); do
  curl -fsS -X POST "http://localhost:8080/api/coupons/${COUPON_ID}/issue" \
    -H "X-User-Id: ${uid}" >/dev/null || true
done

# Worker drain 대기 (간단히 sleep). 더 정확히 보려면 verify_burst.sh 의 폴링 활용.
sleep 3

# 매진 상태 확인 (재고 카운터 0).
remaining="$(docker compose exec -T redis redis-cli GET "coupon:${COUPON_ID}:stock" || echo "?")"
printf '재고 카운터 = %s (0 이어야 매진)\n' "$remaining"
