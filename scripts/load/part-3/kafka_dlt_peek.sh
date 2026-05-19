#!/usr/bin/env bash
# DLT 토픽에 들어온 메시지 확인. 사용: scripts/load/part-3/kafka_dlt_peek.sh
# poison pill 격리 동작을 눈으로 확인할 때 사용.

set -euo pipefail
cd "$(dirname "$0")/../../.."

TOPIC="${TOPIC:-issuance.requested.DLT}"
docker compose exec -T kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic "$TOPIC" \
  --from-beginning --max-messages 20 \
  --property print.headers=true --property print.key=true
