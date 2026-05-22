package com.apiece.coupon.application

import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

// 부하 테스트 측정용 카운터. 캐시 단계별 비교 (DB 도달 횟수, Redis EXISTS 호출 등) 를 위해
// 단계마다 늘려두고 /metrics/cache 엔드포인트로 한 번에 읽는다.
//
// 단순함을 위해 application 단위 in-process AtomicLong 사용. 멀티 인스턴스에서 합산하려면
// 외부 메트릭 시스템 (Prometheus) 으로 넘기는 게 정석이지만, 4단원 부하 측정은 단일 인스턴스
// 베이스에서 단계별 효과를 보는 것이라 이걸로 충분하다.
@Component
class CacheMetrics {
    private val metaDbReads = AtomicLong()
    private val metaCacheHits = AtomicLong()
    private val soldOutRedisExists = AtomicLong()
    private val soldOutFastPathHits = AtomicLong()

    fun incrementMetaDbRead() {
        metaDbReads.incrementAndGet()
    }

    fun incrementMetaCacheHit() {
        metaCacheHits.incrementAndGet()
    }

    fun incrementSoldOutRedisExists() {
        soldOutRedisExists.incrementAndGet()
    }

    fun incrementSoldOutFastPathHit() {
        soldOutFastPathHits.incrementAndGet()
    }

    fun snapshot(): CacheMetricsSnapshot = CacheMetricsSnapshot(
        metaDbReads = metaDbReads.get(),
        metaCacheHits = metaCacheHits.get(),
        soldOutRedisExists = soldOutRedisExists.get(),
        soldOutFastPathHits = soldOutFastPathHits.get(),
    )

    fun reset() {
        metaDbReads.set(0)
        metaCacheHits.set(0)
        soldOutRedisExists.set(0)
        soldOutFastPathHits.set(0)
    }
}

class CacheMetricsSnapshot(
    val metaDbReads: Long,
    val metaCacheHits: Long,
    val soldOutRedisExists: Long,
    val soldOutFastPathHits: Long,
)
