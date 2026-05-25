package com.apiece.coupon.api.dto

import com.apiece.coupon.domain.Coupon
import java.time.LocalDateTime

// 캐시할 정적 필드만. 실시간 재고는 Redis stock, 발급량은 DB 에서 직접.
class CouponResponse(
    val id: Long,
    val name: String,
    val totalQuantity: Int,
    val validityDays: Int,
    val startsAt: LocalDateTime?,
) {
    companion object {
        fun from(coupon: Coupon): CouponResponse = CouponResponse(
            id = requireNotNull(coupon.id),
            name = coupon.name,
            totalQuantity = coupon.totalQuantity,
            validityDays = coupon.validityDays,
            startsAt = coupon.startsAt,
        )
    }
}
