package com.apiece.coupon.application

import com.apiece.coupon.api.dto.CouponResponse
import com.apiece.coupon.domain.CouponRepository
import com.apiece.coupon.infrastructure.cache.CouponCacheRepository
import com.apiece.coupon.support.CouponNotFoundException
import org.springframework.stereotype.Service

@Service
class CouponQueryService(
    private val couponRepository: CouponRepository,
    private val couponCacheRepository: CouponCacheRepository,
) {

    fun get(id: Long): CouponResponse = couponCacheRepository.getOrLoad(id) {
        couponRepository.findById(id)
            .orElseThrow { CouponNotFoundException() }
            .let(CouponResponse::from)
    }
}
