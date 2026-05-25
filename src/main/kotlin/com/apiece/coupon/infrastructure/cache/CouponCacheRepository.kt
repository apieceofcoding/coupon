package com.apiece.coupon.infrastructure.cache

import com.apiece.coupon.api.dto.CouponResponse
import com.apiece.coupon.application.CacheMetrics
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper
import java.time.Duration

@Repository
class CouponCacheRepository(
    private val redis: StringRedisTemplate,
    private val mapper: ObjectMapper,
    private val properties: CacheProperties,
    private val cacheMetrics: CacheMetrics,
) {

    fun getOrLoad(id: Long, loader: () -> CouponResponse): CouponResponse {
        val key = "coupon:$id"
        redis.opsForValue().get(key)?.let { cached ->
            cacheMetrics.incrementCouponCacheHit()
            return mapper.readValue(cached, CouponResponse::class.java)
        }
        cacheMetrics.incrementCouponDbRead()
        val response = loader()
        redis.opsForValue().set(key, mapper.writeValueAsString(response), Duration.ofMillis(properties.ttlMs))
        return response
    }
}
