#!/usr/bin/env bash
# Consumer lag 확인. 사용: scripts/load/part-3/kafka_lag.sh
# 부하 중 다른 셸에서 watch -n 1 로 띄워두면 lag 가 어떻게 차고 빠지는지 본다.

set -euo pipefail
cd "$(dirname "$0")/../../.."

GROUP="${GROUP:-issuance-worker}"
# MSYS_NO_PATHCONV: Git Bash 가 컨테이너 안 경로 /opt/... 를 Windows 경로로
# 바꿔 버리는 것을 막는다. macOS/Linux 에서는 쓰이지 않는 변수라 동작이 같다.
MSYS_NO_PATHCONV=1 docker compose exec -T kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe --group "$GROUP"
