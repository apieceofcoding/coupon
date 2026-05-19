# 부하 테스트

`part-2-1-load-test` 의 k6 시나리오. 같은 시나리오를 `part-2-2-pessimistic-lock`, `part-2-3-redis-lua` 위에서도 그대로 돌려 결과를 비교한다.

## 사전 준비

- `brew install k6 jq`
- `docker compose up -d` 후 서비스가 8080 응답

## 실행 (coupon/ 에서)

```bash
# 1) 과발급 검증 (재고만큼만 발급되어야 한다)
./scripts/load/reset.sh
COUPON_ID=$(scripts/load/create_coupon.sh)
k6 run -e COUPON_ID=$COUPON_ID scripts/load/over_issuance.js
COUPON_ID=$COUPON_ID scripts/load/verify.sh

# 2) 중복발급 검증 (1인 1매만 쿠폰 발급되어야 한다)
./scripts/load/reset.sh
COUPON_ID=$(scripts/load/create_coupon.sh)
k6 run -e COUPON_ID=$COUPON_ID scripts/load/duplicate_issuance.js
COUPON_ID=$COUPON_ID scripts/load/verify.sh
```

## 결과 해석

verify.sh 가 한 표로 보여주는 컬럼:

| 컬럼              | 의미                                            |
| --------------- | --------------------------------------------- |
| issued_quantity | `coupon.issued_quantity` 컬럼 값 (카운터)           |
| total_quantity  | 재고 총량                                         |
| issuance_rows   | 실제로 발급된 `issuance` 행 수                        |
| duplicate_users | 같은 user 가 한 쿠폰을 2번 이상 받은 수                    |
| over_issuance   | `issuance_rows > total_quantity` 면 `FAIL`     |
| count_match     | `issued_quantity = issuance_rows` 면 `OK`      |

`issuance` 테이블의 `UNIQUE (user_id, coupon_id)` 가 같은 사용자가 같은 쿠폰을 2번 INSERT 하는 것을 DB 단에서 차단해요. 그래서 `duplicate_users` 는 항상 0입니다.

- **`over_issuance=FAIL`**: 사용자가 서로 다른 over_issuance 시나리오에서 발생. `isSoldOut()` 검사가 race 라 여러 스레드가 같은 재고를 보고 다 통과 → 재고를 넘는 INSERT.
- **`count_match=FAIL`**: 두 시나리오 모두에서 발생. 동시 트랜잭션의 `UPDATE coupon SET issued_quantity = ?` 가 서로 덮어쓰는 lost update 로 카운터가 실제 발급 수와 어긋남.

## 측정 순서

`part-2-1-load-test` (v0 결함 재현) → `part-2-2-pessimistic-lock` → `part-2-3-redis-lua`. 각 브랜치에서 위 실행을 반복하고 결과를 design 문서 6.3 절 표에 채움.

## part-3-1-load-test: 발급 폭증 (flash event)

3단원 (큐 디커플링) 의 기준 시나리오. 같은 부하 (5000 req/s, 30s) 를 2단원 vs v2a/v2b/v2c 에서 돌려 **P99 응답 시간** 과 **DB 쓰기 QPS** 가 어떻게 달라지는지 비교한다.

```bash
./scripts/load/reset.sh
COUPON_ID=$(scripts/load/create_coupon.sh)
k6 run -e COUPON_ID=$COUPON_ID scripts/load/flash_event.js
COUPON_ID=$COUPON_ID scripts/load/verify_flash.sh
```

- `flash_event.js`: 5000 req/s 30초. k6 가 P99 와 상태 코드 분포를 출력. `issue_latency p(99)<500ms` 임계가 통과하는지 확인.
- `verify_flash.sh`: Worker 가 큐를 다 비울 때까지 폴링한 뒤 (`COUNT(*)` 가 `STABLE_SECONDS` 초간 변화 없으면 확정) DB 상태 출력. 2단원 (동기) 에서는 즉시 확정, v2a/b/c 에서는 몇 초에서 수십 초 더 걸릴 수 있음.

브랜치별 기대값:

| 브랜치              | 발급 건수 | P99           | 비고                          |
| ---------------- | ----- | ------------- | --------------------------- |
| `part-2-3-redis-lua` | 5000  | DB latency 박힘 | 기준 (동기 INSERT)               |
| `part-3-2a-...`  | 5000  | 평탄            | JVM kill 시 손실 발생 가능          |
| `part-3-2b-...`  | 5000  | 평탄            | 본질은 2a 와 동일 (Spring wrapping) |
| `part-3-2c-...`  | 5000  | 평탄            | Kafka 영속 → JVM kill 손실 없음    |

