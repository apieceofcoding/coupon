#!/usr/bin/env bash
# DLT 토픽에 들어온 메시지 확인. 사용: scripts/load/part-3/kafka_dlt_peek.sh
# poison pill 격리 동작을 눈으로 확인할 때 사용.

set -euo pipefail
cd "$(dirname "$0")/../../.."

TOPIC="${TOPIC:-issuance.requested.DLT}"
# MSYS_NO_PATHCONV: Git Bash 가 컨테이너 안 경로 /opt/... 를 Windows 경로로
# 바꿔 버리는 것을 막는다. macOS/Linux 에서는 쓰이지 않는 변수라 동작이 같다.
MSYS_NO_PATHCONV=1 docker compose exec -T kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic "$TOPIC" \
  --from-beginning --max-messages 20 \
  --property print.headers=true --property print.key=true
