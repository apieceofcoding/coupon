# 부하 / 검증 스크립트

part-2 ~ part-3 시나리오 실행 스크립트. 측정값 해석과 설계 배경은 각 단원 design 문서 참고.

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
part-3/   큐 디커플링: issue_burst.js, run.sh, verify_burst.sh
```

## 실행

```bash
# part-2 (동시성)
./scripts/load/part-2/run.sh

# part-3 (큐 디커플링)
./scripts/load/part-3/run.sh
./scripts/load/part-3/run.sh --once
```

`run.sh` 는 `reset → create_coupon → k6 → verify_burst` 를 한 번에 묶고, 기본으로 두 회차를 도는 워밍업 절차까지 자동화한다. 서비스 프로세스는 띄운 채로 두고, 회차 사이에 `reset.sh` 로 DB/Redis 만 비운다.
