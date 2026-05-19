// 측정: "발급 폭증" 시나리오. 사용자 응답 P99 와 DB 쓰기 QPS 를 본다.
// 2단원 (동기) → v2a/v2b/v2c (큐 디커플링) 비교의 기준 시나리오.
//
// USER_POOL 이 재고 (5000) 보다 충분히 커서 대부분의 요청은 매진 (409 SOLD_OUT)
// 응답을 받는다. 우리가 보고 싶은 것은 "응답 시간 분포" 자체이지 발급 성공 수가 아니다.
import http from 'k6/http';
import { Trend, Counter } from 'k6/metrics';

const COUPON_ID = __ENV.COUPON_ID || '1';
const USER_POOL = 20000;
const BASE = __ENV.BASE_URL || 'http://localhost:8080';

const issueLatency = new Trend('issue_latency', true);
const status2xx = new Counter('status_2xx');
const status409 = new Counter('status_409_sold_out_or_dup');
const statusOther = new Counter('status_other');

export const options = {
  scenarios: {
    flash: {
      executor: 'constant-arrival-rate',
      rate: 5000, timeUnit: '1s', duration: '30s',
      preAllocatedVUs: 2000, maxVUs: 5000,
    },
  },
  thresholds: {
    // 큐 디커플링 효과 확인용 임계. v2a/b/c 에서는 통과, 2단원 (동기) 에서는 깨지는 게 정상.
    'issue_latency': ['p(99)<500'],
  },
};

export default function () {
  const userId = Math.floor(Math.random() * USER_POOL) + 1;
  const res = http.post(`${BASE}/api/coupons/${COUPON_ID}/issue`, null, {
    headers: { 'X-User-Id': String(userId) },
  });
  issueLatency.add(res.timings.duration);
  if (res.status >= 200 && res.status < 300) status2xx.add(1);
  else if (res.status === 409) status409.add(1);
  else statusOther.add(1);
}
