#!/usr/bin/env bash
# coupon/issuance TRUNCATE 후 행 수 확인 (둘 다 0 이어야 정상).

# 명령 실패 / 미정의 변수 / 파이프 중간 실패 시 즉시 종료
set -euo pipefail

# docker-compose.yml 위치로 이동 후 mysql 컨테이너 안에서 TRUNCATE + 확인
cd "$(dirname "$0")/../.."

printf '\n\033[1;36m===== coupon, issuance 데이터 리셋 =====\033[0m\n'
docker compose exec -T -e MYSQL_PWD=coupon mysql mysql -ucoupon -t coupon -e "
  SET FOREIGN_KEY_CHECKS=0; TRUNCATE issuance; TRUNCATE coupon; SET FOREIGN_KEY_CHECKS=1;
  SELECT (SELECT COUNT(*) FROM coupon)   AS coupon_rows,
         (SELECT COUNT(*) FROM issuance) AS issuance_rows;
"

# Redis 의 coupon stock 카운터, 발급자 set 도 비워야 회차 사이 잔존 상태가
# 다음 측정에 섞이지 않는다. 부하 테스트용 redis 라 FLUSHDB 가 안전하고 간단.
printf '\n\033[1;36m===== redis 데이터 리셋 (FLUSHDB) =====\033[0m\n'
docker compose exec -T redis redis-cli FLUSHDB
