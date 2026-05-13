package com.apiece.coupon.api.dto

import com.apiece.coupon.domain.Issuance
import com.apiece.coupon.domain.IssuanceStatus
import java.time.LocalDateTime

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
