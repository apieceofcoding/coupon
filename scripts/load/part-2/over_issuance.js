// 검증: "재고만큼만 발급되어야 한다".
// 사용자 20,000명이 30초 동안 초당 5,000번 요청 → 5,000장 재고를 두고 다 같이 경쟁.
// 실제 발급된 issuance 행 수가 5,000장을 넘으면 과발급 결함.
import http from 'k6/http';

const COUPON_ID = __ENV.COUPON_ID || '1';
const USER_POOL = 20000;

export const options = {
  scenarios: { issue: {
    executor: 'constant-arrival-rate',
    rate: 5000, timeUnit: '1s', duration: '30s',
    preAllocatedVUs: 2000, maxVUs: 5000,
  } },
};

export default function () {
  const userId = Math.floor(Math.random() * USER_POOL) + 1;
  http.post(`http://localhost:8080/api/coupons/${COUPON_ID}/issue`, null, {
    headers: { 'X-User-Id': String(userId) },
  });
}
