# 부하 테스트

```
scripts/load/
├── reset.sh / create_coupon.sh   # 공유 헬퍼
├── part-2/                       # 동시성/정확성
│   ├── over_issuance.js          # 5000 req/s 30s, 재고 race
│   ├── duplicate_issuance.js     # 5000 req/s 30s, 1인 1매
│   └── verify.sh                 # issued_quantity vs issuance_rows
└── part-3/                       # 큐 디커플링
    ├── flash_event.js            # 5000 req/s 30s, P99 측정
    └── verify_flash.sh           # Worker 드레인 후 결과
```

사전 준비: `brew install k6 jq` + `docker compose up -d`.

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
./scripts/load/reset.sh
COUPON_ID=$(scripts/load/create_coupon.sh)
k6 run -e COUPON_ID=$COUPON_ID scripts/load/part-3/flash_event.js
COUPON_ID=$COUPON_ID scripts/load/part-3/verify_flash.sh
```

`flash_event.js` 는 `p(99)<500ms` 임계를 건다. 4개 브랜치에서 같은 시나리오를 돌려 비교:

| 브랜치                       | P99 임계  | JVM kill 시 손실 |
| ------------------------- | ------- | ------------- |
| `part-2-3-redis-lua`      | ❌ (기준)  | n/a           |
| `part-3-2a-inmemory-queue` | ✅       | 있음            |
| `part-3-2b-event-listener` | ✅       | 있음 (2a 와 동일)  |
| `part-3-2c-kafka`         | ✅       | 없음 (Kafka 영속) |

JVM kill 측정: 부하 중 다른 셸에서 `docker compose kill -s KILL coupon-service && docker compose up -d coupon-service`. 손실량 = k6 의 2xx 응답 수 − `issuance_rows`.
