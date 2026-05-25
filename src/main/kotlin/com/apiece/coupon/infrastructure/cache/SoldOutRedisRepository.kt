package com.apiece.coupon.infrastructure.cache

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository

@Repository
class SoldOutRedisRepository(
    private val redis: StringRedisTemplate,
) {
    fun isFlagged(couponId: Long): Boolean =
        redis.hasKey("coupon:$couponId:sold_out")
}
