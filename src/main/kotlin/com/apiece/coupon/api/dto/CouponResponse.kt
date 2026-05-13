package com.apiece.coupon.api.dto

import com.apiece.coupon.domain.Coupon
import java.time.LocalDateTime

class CouponResponse(
    val id: Long,
    val name: String,
    val totalQuantity: Int,
    val issuedQuantity: Int,
    val validityDays: Int,
    val startsAt: LocalDateTime?,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(coupon: Coupon): CouponResponse = CouponResponse(
            id = requireNotNull(coupon.id),
            name = coupon.name,
            totalQuantity = coupon.totalQuantity,
            issuedQuantity = coupon.issuedQuantity,
            validityDays = coupon.validityDays,
            startsAt = coupon.startsAt,
            createdAt = coupon.createdAt,
        )
    }
}
