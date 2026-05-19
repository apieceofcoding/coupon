#!/usr/bin/env bash
# Consumer lag 확인. 사용: scripts/load/part-3/kafka_lag.sh
# 부하 중 다른 셸에서 watch -n 1 로 띄워두면 lag 가 어떻게 차고 빠지는지 본다.

set -euo pipefail
cd "$(dirname "$0")/../../.."

GROUP="${GROUP:-issuance-worker}"
docker compose exec -T kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe --group "$GROUP"
