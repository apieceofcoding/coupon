package com.apiece.coupon.api.dto

import com.apiece.coupon.domain.Coupon
import java.time.LocalDateTime

// 캐시 대상이 되는 정적 필드 (이벤트 동안 거의 안 바뀜).
// 실시간 잔여 재고는 Redis `coupon:{id}:stock` 으로 별도 조회.
// 영구 발급량은 DB `coupon.issued_quantity` (집계/정산용) 에서.
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
