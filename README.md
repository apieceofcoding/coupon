# coupon-service

## 준비

macOS:

```bash
brew install k6 jq
```

Windows: 셸 스크립트를 실행하므로 **Git Bash** 에서 진행합니다 (PowerShell, cmd 는 지원하지 않습니다).

```bash
winget install jqlang.jq
winget install k6 --source winget
```

`.sh` 실행 시 `bad interpreter` 오류가 나면 예전에 CRLF 로 clone 한 경우입니다. 줄바꿈을 한 번 정규화합니다.

```bash
git add --renormalize . && git checkout -- .
```

Docker를 실행한 뒤 프로젝트 루트에서 진행합니다.

## 브랜치별 실행 및 테스트

테스트할 브랜치로 이동하고 서비스를 다시 빌드합니다.

```bash
git switch <브랜치 이름>
./gradlew --stop && ./gradlew jibDockerBuild && docker compose up -d
```

| Part | 브랜치 | 테스트 스크립트 |
| --- | --- | --- |
| Part 0 | `main` (별도 Part 0 브랜치 없음) | — |
| Part 1 | `part-1` | — |
| Part 2 | `part-2-1-load-test`<br>`part-2-2-pessimistic-lock`<br>`part-2-3-redis-lua` | `./scripts/load/part-2/run.sh` |
| Part 3 | `part-3-1-load-test`<br>`part-3-2a-inmemory-queue`<br>`part-3-2b-event-listener`<br>`part-3-2c-kafka` | `./scripts/load/part-3/run.sh` |
| Part 4 | `part-4-0-load-test`<br>`part-4-1a-cache-naive`<br>`part-4-1b-single-flight`<br>`part-4-1c-swr`<br>`part-4-2-sold-out-signal` | `./scripts/load/part-4/run.sh` |
| Part 5 | `part-5-0-drift-injection`<br>`part-5-1-dlt-replay`<br>`part-5-2-reconcile` | `./scripts/load/part-5/run.sh` |
| Part 6 | `part-6-0-load-test`<br>`part-6-1-waiting-room`<br>`part-6-2-edge-rate-limit` | `./scripts/load/part-6/run.sh` |

브랜치를 빌드한 뒤 표에 있는 테스트 스크립트를 실행합니다.

## API 호출

```bash
./scripts/api.sh
```

## 종료

```bash
docker compose down -v
```

## 라이선스

Copyright 2026 apieceofcoding

이 저장소의 자체 작성 예제 코드는 [Apache License 2.0](LICENSE)으로 제공됩니다.
외부 라이브러리와 Docker 이미지는 각 프로젝트의 라이선스를 따르며,
Gradle과 Docker를 통해 사용자의 로컬 환경에 직접 설치됩니다.
