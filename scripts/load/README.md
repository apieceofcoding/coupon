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
