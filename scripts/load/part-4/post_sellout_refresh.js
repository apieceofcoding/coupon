// 시나리오 ②: 매진 후 새로고침 폭주. 매진 시그널 효과 측정.
//
// 사전 조건: 재고를 100 장으로 만든 쿠폰을 만든 뒤 100 명이 발급해 매진시킨다.
// 그 다음 60 초 동안 4,000 req/s 의 발급 폭주를 보낸다.
//
// 측정 포인트:
//   - 4-0 ~ 4-1c (시그널 없음): 모든 요청이 Lua 까지 들어와 -1 (매진) 응답.
//   - 4-2 (시그널 적용): 컨트롤러 진입 직후 fast-path 에서 잘려 Lua 호출 0 수렴.
import http from 'k6/http';
import { Trend, Counter } from 'k6/metrics';

const COUPON_ID = __ENV.COUPON_ID || '1';
const USER_POOL = 50000;        // 매진 후 새로고침이라 발급 성공은 0. 사용자 풀은 분포용.
const BASE = __ENV.BASE_URL || 'http://localhost:8080';

const issueLatency = new Trend('issue_latency', true);
const status2xx = new Counter('status_2xx');
const status409 = new Counter('status_409_sold_out_or_dup');
const statusOther = new Counter('status_other');

export const options = {
  scenarios: {
    post_sellout: {
      executor: 'constant-arrival-rate',
      rate: 4000, timeUnit: '1s', duration: '60s',  // 4,000 req/s × 60s = 240,000 회
      preAllocatedVUs: 2000, maxVUs: 5000,
    },
  },
  // 4-2 적용 후 fast-path 거절은 수 ms 안에 끝나야 한다.
  thresholds: {
    'issue_latency': ['p(99)<100'],
  },
};

export default function () {
  // 사용자 ID 는 매진 시킨 100 명 (1~100) 을 피해 100 이상으로 보낸다.
  // 그러지 않으면 중복 발급으로 빨려 들어가서 측정 분포가 흐려진다.
  const userId = Math.floor(Math.random() * USER_POOL) + 101;
  const res = http.post(`${BASE}/api/coupons/${COUPON_ID}/issue`, null, {
    headers: { 'X-User-Id': String(userId) },
  });
  issueLatency.add(res.timings.duration);
  if (res.status >= 200 && res.status < 300) status2xx.add(1);
  else if (res.status === 409) status409.add(1);
  else statusOther.add(1);
}
