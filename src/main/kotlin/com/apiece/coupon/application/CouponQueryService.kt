package com.apiece.coupon.application

import com.apiece.coupon.api.dto.CouponResponse
import com.apiece.coupon.domain.CouponRepository
import com.apiece.coupon.support.CouponNotFoundException
import org.springframework.stereotype.Service

@Service
class CouponQueryService(
    private val couponRepository: CouponRepository,
    private val cacheMetrics: CacheMetrics,
) {

    fun get(id: Long): CouponResponse {
        cacheMetrics.incrementCouponDbRead()
        val coupon = couponRepository.findById(id)
            .orElseThrow { CouponNotFoundException() }
        return CouponResponse.from(coupon)
    }
}
