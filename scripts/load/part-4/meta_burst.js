// 시나리오 ①: 메타 조회 폭증. 캐시 stampede 와 단계별 해소를 보기 위한 측정.
//
// GET /api/coupons/{id} 를 동시 500 명이 60초간 호출. 메타 캐시 TTL 은 환경변수
// COUPON_CACHE_META_TTL_MS=1000 으로 1초 강제. TTL 만료 직후 마다 동시 요청이 같은
// 키로 떠도는 모습을 단계별 (4-0 ~ 4-1c) 로 비교.
//
// 측정 포인트:
//   - p99 응답 시간 (issue_burst 와 같은 Trend 사용)
//   - metaDbReads (시나리오 시작 전 /metrics/cache/reset 으로 0 → 종료 시 /metrics/cache GET)
import http from 'k6/http';
import { Trend, Counter } from 'k6/metrics';

const COUPON_ID = __ENV.COUPON_ID || '1';
const BASE = __ENV.BASE_URL || 'http://localhost:8080';

const metaLatency = new Trend('meta_latency', true);
const status2xx = new Counter('status_2xx');
const statusOther = new Counter('status_other');

export const options = {
  scenarios: {
    meta_burst: {
      executor: 'constant-arrival-rate',
      rate: 500, timeUnit: '1s', duration: '60s',  // 500 req/s × 60s = 30,000 회
      preAllocatedVUs: 500, maxVUs: 1000,
    },
  },
  // single-flight 도입 (4-1b) 이후엔 stampede 윈도우 안의 P99 가 여전히 튀므로
  // threshold 깨지는 게 정상. SWR (4-1c) 에서 통과.
  thresholds: {
    'meta_latency': ['p(99)<200'],
  },
};

export default function () {
  const res = http.get(`${BASE}/api/coupons/${COUPON_ID}`);
  metaLatency.add(res.timings.duration);
  if (res.status >= 200 && res.status < 300) status2xx.add(1);
  else statusOther.add(1);
}
