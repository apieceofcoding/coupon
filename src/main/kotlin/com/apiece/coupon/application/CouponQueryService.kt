package com.apiece.coupon.application

import com.apiece.coupon.api.dto.CouponResponse
import com.apiece.coupon.domain.CouponRepository
import com.apiece.coupon.support.CouponNotFoundException
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.time.Duration

@Service
class CouponQueryService(
    private val couponRepository: CouponRepository,
    private val redis: StringRedisTemplate,
    private val mapper: ObjectMapper,
    private val cacheMetrics: CacheMetrics,
    @Value("\${coupon.cache.ttl-ms}") private val ttlMs: Long,
) {

    fun get(id: Long): CouponResponse {
        val key = cacheKey(id)
        redis.opsForValue().get(key)?.let { cached ->
            cacheMetrics.incrementCouponCacheHit()
            return mapper.readValue(cached, CouponResponse::class.java)
        }
        cacheMetrics.incrementCouponDbRead()
        val coupon = couponRepository.findById(id)
            .orElseThrow { CouponNotFoundException() }
        val response = CouponResponse.from(coupon)
        redis.opsForValue().set(key, mapper.writeValueAsString(response), Duration.ofMillis(ttlMs))
        return response
    }

    private fun cacheKey(id: Long) = "coupon:$id"
}
