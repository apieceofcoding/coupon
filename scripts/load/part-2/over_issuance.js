// 검증: "재고만큼만 발급되어야 한다".
// 사용자 20,000명이 30초 동안 초당 5,000번 요청 → 5,000장 재고를 두고 다 같이 경쟁.
// 실제 발급된 issuance 행 수가 5,000장을 넘으면 과발급 결함.
import http from 'k6/http';

const COUPON_ID = __ENV.COUPON_ID || '1';   // k6 run -e COUPON_ID=... 로 주입
const USER_POOL = 20000;                     // 재고 5000 << 사용자 20000 → 다수 사용자가 동시 경쟁

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
  const userId = Math.floor(Math.random() * USER_POOL) + 1;     // 1 ~ USER_POOL 중 랜덤
  http.post(`http://localhost:8080/api/coupons/${COUPON_ID}/issue`, null, {
    headers: { 'X-User-Id': String(userId) },                    // 인증 대신 헤더로 사용자 식별
  });
}
