package com.apiece.coupon.api

import com.apiece.coupon.api.dto.CacheMetricsResponse
import com.apiece.coupon.application.CacheMetrics
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// 4단원 부하 테스트 측정 보조. 시나리오 직전에 /metrics/cache/reset 으로 카운터를 0 으로
// 맞추고, 시나리오 끝나면 /metrics/cache 로 값을 읽어 단계별 결과 표를 채운다.
//
// 운영 코드라기보다 강의용 측정 도구라, Prometheus 같은 외부 시스템 연동 대신
// 단순한 in-process 엔드포인트로 만들어 둔다.
@RestController
@RequestMapping("/metrics/cache")
class CacheMetricsController(
    private val cacheMetrics: CacheMetrics,
) {

    @GetMapping
    fun snapshot(): CacheMetricsResponse =
        CacheMetricsResponse.from(cacheMetrics.snapshot())

    @PostMapping("/reset")
    fun reset() {
        cacheMetrics.reset()
    }
}
