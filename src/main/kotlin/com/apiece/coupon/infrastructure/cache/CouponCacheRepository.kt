package com.apiece.coupon.infrastructure.cache

import com.apiece.coupon.api.dto.CouponResponse
import com.apiece.coupon.application.CacheMetrics
import org.springframework.core.io.ClassPathResource
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper
import java.time.Duration
import java.util.UUID

@Repository
class CouponCacheRepository(
    private val redis: StringRedisTemplate,
    private val mapper: ObjectMapper,
    private val properties: CacheProperties,
    private val cacheMetrics: CacheMetrics,
) {

    private val singleFlightScript: RedisScript<List<*>> = RedisScript.of(
        ClassPathResource("lua/cache-single-flight.lua"),
        List::class.java,
    )

    fun getOrLoad(id: Long, loader: () -> CouponResponse): CouponResponse {
        val cacheKey = "coupon:$id"
        val lockKey = "coupon:$id:lock"
        val fallbackKey = "coupon:$id:fallback"
        val token = UUID.randomUUID().toString()
        repeat(MAX_RETRIES) {
            @Suppress("UNCHECKED_CAST")
            val result = redis.execute(
                singleFlightScript,
                listOf(cacheKey, lockKey, fallbackKey),
                token, LOCK_TTL_MS.toString(),
            ) as List<String>

            when (result[0]) {
                "HIT", "WAIT_FALLBACK" -> {
                    cacheMetrics.incrementCouponCacheHit()
                    return mapper.readValue(result[1], CouponResponse::class.java)
                }
                "LOAD" -> return try {
                    cacheMetrics.incrementCouponDbRead()
                    val response = loader()
                    val json = mapper.writeValueAsString(response)
                    redis.opsForValue().set(cacheKey, json, Duration.ofMillis(properties.ttlMs))
                    redis.opsForValue().set(fallbackKey, json, Duration.ofMillis(properties.ttlMs * FALLBACK_TTL_MULTIPLIER))
                    response
                } finally {
                    redis.delete(lockKey)
                }
                "WAIT_MISS" -> Thread.sleep(WAIT_BACKOFF_MS)
            }
        }
        throw IllegalStateException("쿠폰 캐시 채우기 timeout (id=$id)")
    }

    private companion object {
        const val MAX_RETRIES = 50
        const val WAIT_BACKOFF_MS = 20L
        const val LOCK_TTL_MS = 3_000L
        const val FALLBACK_TTL_MULTIPLIER = 60L
    }
}
