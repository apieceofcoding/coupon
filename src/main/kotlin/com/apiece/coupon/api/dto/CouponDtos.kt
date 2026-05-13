package com.apiece.coupon.api.dto

import com.apiece.coupon.domain.Coupon
import com.apiece.coupon.domain.Issuance
import com.apiece.coupon.domain.IssuanceStatus
import java.time.LocalDateTime

class CreateCouponRequest(
    val name: String,
    val totalQuantity: Int = 5000,
    val validityDays: Int = 7,
    val startsAt: LocalDateTime? = null,
)

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

class IssuanceResponse(
    val id: Long,
    val userId: Long,
    val couponId: Long,
    val status: IssuanceStatus,
    val issuedAt: LocalDateTime,
    val expiresAt: LocalDateTime,
    val usedAt: LocalDateTime?,
) {
    companion object {
        fun from(issuance: Issuance): IssuanceResponse = IssuanceResponse(
            id = requireNotNull(issuance.id),
            userId = issuance.userId,
            couponId = issuance.couponId,
            status = issuance.status,
            issuedAt = issuance.issuedAt,
            expiresAt = issuance.expiresAt,
            usedAt = issuance.usedAt,
        )
    }
}
