package com.apiece.coupon.application

import com.apiece.coupon.api.dto.CouponResponse
import com.apiece.coupon.domain.CouponRepository
import com.apiece.coupon.infrastructure.cache.CacheProperties
import com.apiece.coupon.infrastructure.cache.CouponCacheRepository
import com.apiece.coupon.support.CouponNotFoundException
import org.springframework.stereotype.Service

@Service
class CouponQueryService(
    private val couponRepository: CouponRepository,
    private val couponCacheRepository: CouponCacheRepository,
    private val cacheProperties: CacheProperties,
) {

    fun get(id: Long): CouponResponse = couponCacheRepository.getOrLoad(id) {
        if (cacheProperties.simulatedLoadLatencyMs > 0) {
            Thread.sleep(cacheProperties.simulatedLoadLatencyMs)
        }
        couponRepository.findById(id)
            .orElseThrow { CouponNotFoundException() }
            .let(CouponResponse::from)
    }
}
