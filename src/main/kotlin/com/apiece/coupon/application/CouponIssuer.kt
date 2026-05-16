package com.apiece.coupon.application

import com.apiece.coupon.support.AlreadyIssuedException
import com.apiece.coupon.support.SoldOutException
import org.springframework.core.io.ClassPathResource
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Component

@Component
class CouponIssuer(
    private val redisTemplate: StringRedisTemplate,
) {

    private val script: RedisScript<Long> = RedisScript.of(
        ClassPathResource("lua/issue.lua"),
        Long::class.java,
    )

    // 사용자 중복 검증 + 재고 검증 + 재고 차감 + 사용자 등록을 Lua 한 덩어리로 atomic 실행.
    // 실패 시 도메인 예외를 던진다.
    fun tryIssue(couponId: Long, userId: Long) {
        val raw = redisTemplate.execute(
            script,
            listOf(stockKey(couponId), usersKey(couponId)),
            userId.toString(),
        ) ?: error("Lua 스크립트 결과가 null")

        when (raw) {
            1L -> Unit
            0L -> throw SoldOutException()
            -1L -> throw AlreadyIssuedException()
            else -> error("예상치 못한 Lua 결과: $raw")
        }
    }

    fun initStock(couponId: Long, totalQuantity: Int) {
        redisTemplate.opsForValue().set(stockKey(couponId), totalQuantity.toString())
    }

    private fun stockKey(couponId: Long) = "coupon:$couponId:stock"
    private fun usersKey(couponId: Long) = "coupon:$couponId:users"
}
