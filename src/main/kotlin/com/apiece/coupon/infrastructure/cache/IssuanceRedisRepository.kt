package com.apiece.coupon.infrastructure.cache

import org.springframework.core.io.ClassPathResource
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Repository

@Repository
class IssuanceRedisRepository(
    private val redis: StringRedisTemplate,
    private val soldOutProperties: SoldOutProperties,
) {

    private val issueScript: RedisScript<Long> = RedisScript.of(
        ClassPathResource("lua/issue.lua"),
        Long::class.java,
    )

    // 반환: 1=성공, 0=매진, -1=중복 발급
    fun tryIssue(couponId: Long, userId: Long): Long =
        redis.execute(
            issueScript,
            listOf("coupon:$couponId:stock", "coupon:$couponId:users", "coupon:$couponId:sold_out"),
            userId.toString(),
            soldOutProperties.ttlSeconds.toString(),
        ) ?: error("Lua 스크립트 결과가 null")

    fun initStock(couponId: Long, totalQuantity: Int) {
        redis.opsForValue().set("coupon:$couponId:stock", totalQuantity.toString())
    }
}
