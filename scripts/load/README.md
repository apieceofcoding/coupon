# 부하 테스트

```
scripts/load/
├── reset.sh / create_coupon.sh   # 공유 헬퍼
├── part-2/                       # 동시성/정확성
│   ├── over_issuance.js          # 5000 req/s 30s, 재고 race
│   ├── duplicate_issuance.js     # 5000 req/s 30s, 1인 1매
│   └── verify.sh                 # issued_quantity vs issuance_rows
└── part-3/                       # 큐 디커플링
    ├── issue_burst.js            # 5000 req/s 30s, P99 측정
    ├── verify_burst.sh           # Worker 드레인 후 결과
    └── run.sh                    # 워밍업 + 본 측정 통합 러너
```

사전 준비: `brew install k6 jq` + `docker compose up -d`.

### 브랜치 전환 시 (이미지 재생성)

브랜치마다 서비스 코드가 다르므로, 전환 후에는 **이미지를 새로 굽고 `coupon-service` 컨테이너만 교체**한다. mysql/redis/kafka 는 그대로 둔다.

```bash
git checkout <branch>
./gradlew jibDockerBuild                                # coupon-service:latest 재생성 (Jib)
docker compose up -d --force-recreate coupon-service    # 새 이미지로 컨테이너만 교체
```

이걸 빼먹으면 이전 브랜치의 코드가 그대로 돌아 측정값이 헷갈린다. 재시작 후 `docker compose logs -f coupon-service` 로 `Started CouponServiceApplication` 한 줄 뜨는 것까지 확인하고 부하 시나리오를 돌리자.

## part-2

```bash
./scripts/load/reset.sh
COUPON_ID=$(scripts/load/create_coupon.sh)
k6 run -e COUPON_ID=$COUPON_ID scripts/load/part-2/over_issuance.js
COUPON_ID=$COUPON_ID scripts/load/part-2/verify.sh
```

`over_issuance=FAIL` 은 재고 race, `count_match=FAIL` 은 카운터 lost update. 자세한 해석은 [2단원 design](../../../../materials/domain/02-coupon-concurrency-design.md).

## part-3

```bash
./scripts/load/part-3/run.sh           # 워밍업 1회 + 본 측정 1회 (기본)
./scripts/load/part-3/run.sh --once    # 한 회차만
```

`run.sh` 는 `reset → create_coupon → k6 → verify_burst` 를 한 번에 묶고, 기본으로 두 회차를 도는 워밍업 절차까지 자동화한다. 서비스 프로세스는 띄운 채로 두고, 회차 사이에 `reset.sh` 로 DB/Redis 만 비우므로 JVM JIT, HikariCP 풀, Lettuce/Kafka 컨슈머 상태는 살아남아 2회차가 steady-state 값이 된다. 2-3 (동기) 만 예외로 두 회차가 같은데, 병목이 JIT/풀이 아니라 DB INSERT 자체라 워밍업이 의미 없기 때문이다.

### 측정 결과 (5000 req/s × 30s, M-series Mac + Docker Desktop)

> 발급 5000, 2xx 5000, 409 ~145k 는 4개 브랜치 모두 동일.

| 브랜치                      | P99 (워밍업) | P99 (steady) | 임계 (500ms) | 비고                              |
| ------------------------- | --------- | ------------ | --------- | ------------------------------- |
| `part-2-3-redis-lua`      | 4.61s     | (워밍업 무관, DB 동기)   | ❌ 기준    | 응답이 DB INSERT 끝날 때까지 매달림           |
| `part-3-2a-inmemory-queue`| 425ms     | **3.79ms**   | ✅         | LinkedBlockingQueue 한 단계로 1000배 ↓ |
| `part-3-2b-event-listener`| 435ms     | **3.98ms**   | ✅         | 2a 와 본질 동일, 어노테이션만 다름             |
| `part-3-2c-kafka`         | 592ms     | **5.19ms**   | ✅         | 워밍업이 더 큼 (Kafka 컨슈머 그룹 조인)        |

### JVM kill 시 손실 측정 (별도 절차)

부하 중 다른 셸에서 `docker compose kill -s KILL coupon-service && docker compose up -d coupon-service`. 손실량 = k6 의 2xx 응답 수 − `issuance_rows`.

- 2a / 2b: 큐 안 메시지 통째 손실 (양수)
- 2c: Kafka 가 미커밋 offset 부터 재처리 → 손실 0
