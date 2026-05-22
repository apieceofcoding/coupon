// 검증: "1인 1매만 쿠폰 발급되어야 한다".
// 사용자 1,000명이 30초 동안 초당 5,000번 요청 → 같은 사람이 여러 번 시도하게 됨.
// coupon.issued_quantity 와 실제 issuance 행 수가 다르면 발급 처리 중 race 가 일어난 것.
import http from 'k6/http';

const COUPON_ID = __ENV.COUPON_ID || '1';   // k6 run -e COUPON_ID=... 로 주입
const USER_POOL = 1000;                      // 풀이 작아서 동일 userId 가 여러 번 뽑힘 (중복 발급 유도)

export const options = {
  scenarios: { issue: {
    // constant-arrival-rate: 백엔드 응답 속도와 무관하게 "초당 N건" 을 강제로 쏟아부음.
    executor: 'constant-arrival-rate',
    rate: 5000, timeUnit: '1s', duration: '30s', // 5000 req/s × 30s = 총 150,000 요청
    preAllocatedVUs: 2000, maxVUs: 5000,         // 동시 처리용 VU 풀 (호출 횟수는 rate 가 결정)
  } },
};

// 각 VU 가 반복 실행하는 단위.
export default function () {
  const userId = Math.floor(Math.random() * USER_POOL) + 1;     // 1 ~ USER_POOL 중 랜덤 (반복 등장 의도적)
  http.post(`http://localhost:8080/api/coupons/${COUPON_ID}/issue`, null, {
    headers: { 'X-User-Id': String(userId) },                    // 같은 userId 가 여러 요청에 등장 가능
  });
}
