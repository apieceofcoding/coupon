// 검증: "1인 1매만 쿠폰 발급되어야 한다".
// 사용자 1,000명이 30초 동안 초당 5,000번 요청 → 같은 사람이 여러 번 시도하게 됨.
// coupon.issued_quantity 와 실제 issuance 행 수가 다르면 발급 처리 중 race 가 일어난 것.
import http from 'k6/http';

const COUPON_ID = __ENV.COUPON_ID || '1';
const USER_POOL = 1000;

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
