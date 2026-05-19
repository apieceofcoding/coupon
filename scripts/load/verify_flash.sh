#!/usr/bin/env bash
# flash_event 시나리오 검증. Worker 드레인을 폴링으로 기다린 뒤 결과 표 출력.
#
# 사용:  COUPON_ID=1 scripts/load/verify_flash.sh
# 환경:  TIMEOUT_SECONDS (기본 60)  STABLE_SECONDS (기본 3)

set -euo pipefail
COUPON_ID="${COUPON_ID:-1}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-60}"
STABLE_SECONDS="${STABLE_SECONDS:-3}"
cd "$(dirname "$0")/../.."

ROW_COUNT_SQL="SELECT COUNT(*) FROM issuance WHERE coupon_id = $COUPON_ID"
row_count() {
  docker compose exec -T -e MYSQL_PWD=coupon mysql \
    mysql -ucoupon -BN coupon -e "$ROW_COUNT_SQL"
}

printf '\033[1;36m===== Worker 드레인 대기 (최대 %ss, %ss 안정 시 확정) =====\033[0m\n' \
  "$TIMEOUT_SECONDS" "$STABLE_SECONDS"

prev="$(row_count)"
stable=0
elapsed=0
while (( elapsed < TIMEOUT_SECONDS )); do
  sleep 1
  current="$(row_count)"
  if [[ "$current" == "$prev" ]]; then
    stable=$((stable + 1))
    if (( stable >= STABLE_SECONDS )); then break; fi
  else
    stable=0
    printf '  +%ss issuance rows: %s\n' "$elapsed" "$current"
  fi
  prev="$current"
  elapsed=$((elapsed + 1))
done
printf '  드레인 완료. issuance rows = %s\n\n' "$prev"

SQL="
  SELECT
    (SELECT issued_quantity FROM coupon   WHERE id        = $COUPON_ID) AS issued_quantity,
    (SELECT total_quantity  FROM coupon   WHERE id        = $COUPON_ID) AS total_quantity,
    (SELECT COUNT(*)        FROM issuance WHERE coupon_id = $COUPON_ID) AS issuance_rows,
    IF((SELECT COUNT(*) FROM issuance WHERE coupon_id = $COUPON_ID)
        > (SELECT total_quantity FROM coupon WHERE id = $COUPON_ID), 'FAIL', 'OK') AS over_issuance,
    IF((SELECT issued_quantity FROM coupon WHERE id = $COUPON_ID)
        = (SELECT COUNT(*) FROM issuance WHERE coupon_id = $COUPON_ID), 'OK', 'FAIL') AS count_match;
"
docker compose exec -T -e MYSQL_PWD=coupon mysql mysql -ucoupon -t coupon -e "$SQL"

read field total rows over count < <(docker compose exec -T -e MYSQL_PWD=coupon mysql \
  mysql -ucoupon -BN coupon -e "$SQL")

if [[ "$over" == "FAIL" ]]; then
  printf '\033[1;31mFAIL: 과발급 (실제발급 %s > 재고 %s)\033[0m\n' "$rows" "$total"
elif [[ "$count" == "FAIL" ]]; then
  printf '\033[1;33mWARN: 카운터 불일치 (issued_quantity %s, 실제발급 %s)\033[0m\n' "$field" "$rows"
  printf '  → Worker 가 issued_quantity 까지 갱신하는지 확인. v2 에서는 같이 가야 함.\n'
else
  printf '\033[1;32mPASS: issuance_rows %s, issued_quantity %s, total %s\033[0m\n' \
    "$rows" "$field" "$total"
fi
