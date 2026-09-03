# 부하 / 검증 스크립트

part-2 ~ part-5 시나리오 실행 스크립트. 측정값 해석과 설계 배경은 각 단원 design 문서 참고.

## 사전 준비

```bash
brew install k6 jq
docker compose up -d
```

Windows 는 Git Bash 에서 실행한다. jq, k6 는 `winget install jqlang.jq`, `winget install k6 --source winget` 로 설치한다.

## 브랜치 전환 시

브랜치마다 서비스 코드가 다르다. 전환하면 이미지를 새로 굽고 컨테이너만 교체한다 (mysql/redis/kafka 는 유지).

```bash
git checkout <branch>
./gradlew jibDockerBuild
docker compose up -d --force-recreate coupon-service
```

DLT 상태 이름을 바꾼 p5-1 이상을 기존 DB 볼륨에서 처음 실행할 때는 학습용 데이터이므로 한 번만 `docker compose down -v` 후 다시 올린다.

`issuance`는 사용자와 쿠폰별 한 행을 유지한다.

## 디렉토리

```
공유      reset.sh, create_coupon.sh
part-2/   동시성: over_issuance.js, run.sh, verify.sh
part-3/   큐 디커플링: issue_burst.js, verify_burst.sh, run.sh, kafka_lag.sh, kafka_dlt_peek.sh
part-4/   캐시+매진 상태: coupon_burst.js, post_sellout_refresh.js, sell_out.sh, run.sh
part-5/   DLT 재처리+대사: force_dlt.sh, force_db_only.sh, drift_report.sh, run.sh
```

## 실행

```bash
# part-2 (동시성)
./scripts/load/part-2/run.sh

# part-3 (큐 디커플링): reset, create, k6, verify 통합 러너
./scripts/load/part-3/run.sh
scripts/load/part-3/kafka_dlt_peek.sh   # DLT 확인 (3-2c)

# part-4 (캐시 + 매진 상태)
./scripts/load/part-4/run.sh            # coupon | sellout | all

# part-5: 현재 브랜치의 5-0, 5-1, 5-2 시나리오를 자동 선택
./scripts/load/part-5/run.sh
```

part-5 는 두 등식 `total = 발급누적 + Redis재고 = Redis사용자 + Redis재고` 의 잔차로 불일치를 잰다. `force_dlt` 는 DB 측(알람 대상), `force_db_only` 는 목록 측(자동 보정 대상)을 깬다.

DLT consumer는 실패한 메시지 원문과 Kafka 오류 메시지를 `issuance_dlt_log`에 기록한다. 운영자는 `GET /admin/issuance/dlt`로 확인하고, 원인을 해결한 뒤 `POST /admin/issuance/dlt/replay`에 `{"ids": [1, 2]}`를 보내 선택한 메시지를 재처리한다.

최근 발급 기록은 1분마다 대사한다. `/admin/reconcile/run`은 Redis 최근 발급 기록에 의존하지 않고 전체 쿠폰을 즉시 대사한다.
