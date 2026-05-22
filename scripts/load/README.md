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

`run.sh` 는 `reset → create_coupon → k6 → verify_burst` 를 한 번에 묶고, 기본으로 두 회차를 도는 워밍업 절차까지 자동화한다. 서비스 프로세스는 띄운 채로 두고, 회차 사이에 `reset.sh` 로 DB/Redis 만 비우므로 JVM JIT, HikariCP 풀, Lettuce/Kafka 컨슈머 상태는 살아남아 2회차가 steady-state 값이 된다. 3-1 (동기) 만 예외로 두 회차가 비슷한데, 병목이 JIT/풀이 아니라 DB INSERT 자체라 워밍업이 의미 없기 때문이다.

### 측정 결과 (5000 req/s × 30s, M-series Mac + Docker Desktop, 로깅 INFO 기준)

> 발급 5000, 2xx 5000, 409 ~145k 는 4개 브랜치 모두 동일. 측정은 매 브랜치마다 `git checkout` + `./gradlew jibDockerBuild` + `docker compose up -d --force-recreate coupon-service` 후 `run.sh` 한 번 실행.

| 브랜치                       | P99 (워밍업) | P99 (steady) | dropped (steady) | 임계 (500ms) | 비고                                  |
| -------------------------- | --------- | ------------ | ---------------- | --------- | ----------------------------------- |
| `part-3-1-load-test`       | 5.22s     | 4.27s        | 21,931           | ❌ 기준    | 응답이 DB INSERT 끝날 때까지 매달림              |
| `part-3-2a-inmemory-queue` | 362ms     | **5.33ms**   | ~0               | ✅         | LinkedBlockingQueue 한 단계로 800배 ↓     |
| `part-3-2b-event-listener` | 429ms     | **4.39ms**   | ~0               | ✅         | 2a 와 본질 동일, 어노테이션만 다름                |
| `part-3-2c-kafka`          | 535ms     | **8.00ms**   | ~0               | ✅         | 워밍업이 가장 큼 (Kafka 컨슈머 그룹 조인 + 메타 페치)  |

dropped_iterations 도 같이 봐야 디커플링 효과가 입체적이다. 3-1 은 백엔드가 느려서 k6 VU 풀 5000 이 가득 차고 21k+ 요청이 발사조차 못 한다. 3-2a/b/c 는 백엔드가 빨라 VU 가 다음 iteration 으로 즉시 넘어가 dropped 가 0 에 수렴.

**알려진 정확성 차이**: 3-2a/b/c 는 `verify_burst.sh` 가 `count_match=FAIL` 을 띄운다. 이유는 발급 흐름을 큐로 떼어내면서 `coupon.issued_quantity` 컬럼 갱신을 Worker 가 안 하기 때문 (Worker 는 `issuance` row 만 INSERT). `issuance_rows=5000` 은 정확하므로 실제 발급은 정확하고, 카운터 컬럼이 빠진 것뿐. Worker 의 책임에 카운터 증가를 넣을지, `issued_quantity` 컬럼을 폐기하고 `COUNT(*)` 로 대체할지는 도메인 결정이다.

### JVM kill 시 손실 측정 (별도 절차)

부하 중 다른 셸에서 `docker compose kill -s KILL coupon-service && docker compose up -d coupon-service`. 손실량 = k6 의 2xx 응답 수 − `issuance_rows`.

- 2a / 2b: 큐 안 메시지 통째 손실 (양수)
- 2c: Kafka 가 미커밋 offset 부터 재처리 → 손실 0
