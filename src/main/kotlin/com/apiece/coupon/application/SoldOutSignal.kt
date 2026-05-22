package com.apiece.coupon.application

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

// 4-2 Application fast-path. 매진 플래그는 한 번 set 되면 이벤트 종료까지 그대로라는
// 성질을 활용해, JVM 안에 1초 TTL in-process 캐시를 둔다. Redis EXISTS 호출이 인스턴스
// 당 초당 1 회로 떨어진다.
//
// 누수 (false negative): 매진 직후 1 초 동안은 fast-path 가 매진을 못 잡고 Lua 까지
// 흘려보낸다. 그래도 Lua 가 마지막에 -1 로 거절하므로 정확성은 안 깨진다.
@Component
class SoldOutSignal(
    private val redis: StringRedisTemplate,
    private val cacheMetrics: CacheMetrics,
    @Value("\${coupon.sold-out.fast-path-ttl-ms}") fastPathTtlMs: Long,
) {

    private val cache: LoadingCache<Long, Boolean> = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMillis(fastPathTtlMs))
        .maximumSize(MAX_TRACKED_COUPONS)
        .build { couponId ->
            cacheMetrics.incrementSoldOutRedisExists()
            redis.hasKey("coupon:$couponId:sold_out")
        }

    fun isSoldOut(couponId: Long): Boolean {
        val soldOut = cache.get(couponId) ?: false
        if (soldOut) cacheMetrics.incrementSoldOutFastPathHit()
        return soldOut
    }

    private companion object {
        const val MAX_TRACKED_COUPONS = 1_000L
    }
}
