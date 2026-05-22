package com.apiece.coupon.api.dto

import com.apiece.coupon.domain.Coupon
import java.time.LocalDateTime

// 메타 = 이벤트 기간 동안 사실상 변하지 않는 부분 (이름, 총 수량, 유효기간).
// 잔여 재고와 발급량 같은 실시간 값은 여기 포함하지 않는다.
class CouponMetaResponse(
    val id: Long,
    val name: String,
    val totalQuantity: Int,
    val validityDays: Int,
    val startsAt: LocalDateTime?,
) {
    companion object {
        fun from(coupon: Coupon): CouponMetaResponse = CouponMetaResponse(
            id = requireNotNull(coupon.id),
            name = coupon.name,
            totalQuantity = coupon.totalQuantity,
            validityDays = coupon.validityDays,
            startsAt = coupon.startsAt,
        )
    }
}
