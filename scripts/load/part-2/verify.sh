#!/usr/bin/env bash
# 발급량과 결함 확인. 사용: COUPON_ID=1 scripts/load/part-2/verify.sh

# 명령 실패 / 미정의 변수 / 파이프 중간 실패 시 즉시 종료
set -euo pipefail
COUPON_ID="${COUPON_ID:-1}"
cd "$(dirname "$0")/../../.."

# issued_quantity  coupon.issued_quantity (카운터 필드)
# total_quantity   재고 총량
# issuance_rows    실제 발급된 issuance 행 수
# duplicate_users  같은 user 가 같은 coupon 을 2번 이상 받은 수 (UNIQUE 있으면 항상 0)
# over_issuance    issuance_rows > total_quantity  → 전역 N장 깨짐
# count_match      issued_quantity = issuance_rows → 카운터가 실제와 맞음 (race 없음)
SQL="
  SELECT
    v.issued_quantity, v.total_quantity, v.issuance_rows, v.duplicate_users,
    IF(v.issuance_rows > v.total_quantity, 'FAIL', 'OK') AS over_issuance,
    IF(v.issued_quantity = v.issuance_rows, 'OK', 'FAIL') AS count_match
  FROM (
    SELECT
      (SELECT issued_quantity FROM coupon   WHERE id        = $COUPON_ID) AS issued_quantity,
      (SELECT total_quantity  FROM coupon   WHERE id        = $COUPON_ID) AS total_quantity,
      (SELECT COUNT(*)        FROM issuance WHERE coupon_id = $COUPON_ID) AS issuance_rows,
      (SELECT COUNT(*) FROM (SELECT user_id FROM issuance WHERE coupon_id = $COUPON_ID
        GROUP BY user_id HAVING COUNT(*) > 1) t)                          AS duplicate_users
  ) v;
"

# 1) 표 출력
docker compose exec -T -e MYSQL_PWD=coupon mysql mysql -ucoupon -t coupon -e "$SQL"

# 2) 같은 SQL 을 batch 모드로 다시 받아 변수에 넣고 한국어 판정 출력
read field total rows _dup over count < <(docker compose exec -T -e MYSQL_PWD=coupon mysql \
  mysql -ucoupon -BN coupon -e "$SQL")

if   [[ "$over"  == "FAIL" ]]; then
  printf '\033[1;31mFAIL: 과발급 (실제발급 %s > 재고 %s)\033[0m\n' "$rows" "$total"
elif [[ "$count" == "FAIL" ]]; then
  printf '\033[1;31mFAIL: 카운터 불일치 (issued_quantity %s, 실제발급 %s)\033[0m\n' "$field" "$rows"
else
  printf '\033[1;32mPASS\033[0m\n'
fi
