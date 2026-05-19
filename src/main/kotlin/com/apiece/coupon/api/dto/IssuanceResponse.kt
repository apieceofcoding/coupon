package com.apiece.coupon.api.dto

import com.apiece.coupon.domain.Issuance
import com.apiece.coupon.domain.IssuanceStatus
import java.time.LocalDateTime

class IssuanceResponse(
    // 비동기 발급 흐름에서는 DB INSERT 이전에 응답이 나가서 id 가 아직 없을 수 있다.
    val id: Long?,
    val userId: Long,
    val couponId: Long,
    val status: IssuanceStatus,
    val issuedAt: LocalDateTime,
    val expiresAt: LocalDateTime,
    val usedAt: LocalDateTime?,
) {
    companion object {
        fun from(issuance: Issuance): IssuanceResponse = IssuanceResponse(
            id = issuance.id,
            userId = issuance.userId,
            couponId = issuance.couponId,
            status = issuance.status,
            issuedAt = issuance.issuedAt,
            expiresAt = issuance.expiresAt,
            usedAt = issuance.usedAt,
        )
    }
}
