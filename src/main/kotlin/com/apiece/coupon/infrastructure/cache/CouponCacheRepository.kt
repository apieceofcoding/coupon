package com.apiece.coupon.infrastructure.cache

import com.apiece.coupon.api.dto.CouponResponse
import com.apiece.coupon.application.CacheMetrics
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

@Repository
class CouponCacheRepository(
    private val redis: StringRedisTemplate,
    private val mapper: ObjectMapper,
    private val properties: CacheProperties,
    private val cacheMetrics: CacheMetrics,
) {

    // 사용자 응답 스레드를 막지 않으려고 백그라운드 갱신은 별도 풀에서.
    private val backgroundExecutor = Executors.newFixedThreadPool(4) { r ->
        Thread(r, "coupon-cache-swr-refresh").apply { isDaemon = true }
    }

    private val lookupScript = listLuaScript("lua/cache-swr-lookup.lua")

    fun getOrLoad(id: Long, loader: () -> CouponResponse): CouponResponse {
        val cacheKey = "coupon:$id"
        val lockKey = "coupon:$id:lock"
        repeat(MAX_RETRIES) {
            val token = UUID.randomUUID().toString()
            val now = Instant.now().toEpochMilli()
            val result = redis.runForStrings(
                lookupScript,
                listOf(cacheKey, lockKey),
                now, properties.freshMs, token, LOCK_TTL_MS,
            )

            when (result[0]) {
                "HIT" -> {
                    cacheMetrics.incrementCouponCacheHit()
                    return mapper.readValue(result[1], CouponResponse::class.java)
                }
                "STALE_REFRESH" -> {
                    cacheMetrics.incrementCouponCacheHit()
                    backgroundExecutor.execute {
                        try { fillCache(id, lockKey, loader) }
                        catch (e: Exception) { log.warn { "백그라운드 SWR 갱신 실패 (id=$id): ${e.message}" } }
                    }
                    return mapper.readValue(result[1], CouponResponse::class.java)
                }
                "LOAD" -> return fillCache(id, lockKey, loader)
                "WAIT" -> Thread.sleep(WAIT_BACKOFF_MS)
            }
        }
        throw IllegalStateException("쿠폰 캐시 채우기 timeout (id=$id)")
    }

    private fun fillCache(id: Long, lockKey: String, loader: () -> CouponResponse): CouponResponse {
        try {
            cacheMetrics.incrementCouponDbRead()
            val response = loader()
            val key = "coupon:$id"
            redis.opsForHash<String, String>().putAll(key, mapOf(
                "value" to mapper.writeValueAsString(response),
                "fetchedAtMs" to Instant.now().toEpochMilli().toString(),
            ))
            redis.expire(key, properties.ttlMs, TimeUnit.MILLISECONDS)
            return response
        } finally {
            redis.delete(lockKey)
        }
    }

    @PreDestroy
    fun shutdown() {
        backgroundExecutor.shutdown()
    }

    private companion object {
        const val MAX_RETRIES = 50
        const val WAIT_BACKOFF_MS = 20L
        const val LOCK_TTL_MS = 3_000L
    }
}
