package com.apiece.coupon.api.dto

import java.time.LocalDateTime

class CreateCouponRequest(
    val name: String,
    val totalQuantity: Int = 5000,
    val validityDays: Int = 7,
    val startsAt: LocalDateTime? = null,
)
