package com.apiece.coupon.application

import com.apiece.coupon.infrastructure.cache.SoldOutProperties
import com.apiece.coupon.infrastructure.cache.SoldOutRedisRepository
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import org.springframework.stereotype.Component
import java.time.Duration

// 매진 플래그를 in-process 로 캐싱해 Redis EXISTS 를 인스턴스 당 초당 1회로 줄임.
// fast-path TTL 만큼의 false negative 는 Lua 가 -1 로 최종 거절해서 잡아낸다.
@Component
class SoldOutSignal(
    soldOutRedisRepository: SoldOutRedisRepository,
    private val cacheMetrics: CacheMetrics,
    properties: SoldOutProperties,
) {

    private val cache: LoadingCache<Long, Boolean> = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMillis(properties.fastPathTtlMs))
        .maximumSize(MAX_TRACKED_COUPONS)
        .build { couponId ->
            cacheMetrics.incrementSoldOutRedisExists()
            soldOutRedisRepository.isFlagged(couponId)
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
