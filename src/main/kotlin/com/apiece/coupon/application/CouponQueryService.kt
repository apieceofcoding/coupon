package com.apiece.coupon.application

import com.apiece.coupon.api.dto.CouponMetaResponse
import com.apiece.coupon.domain.CouponRepository
import com.apiece.coupon.support.CouponNotFoundException
import org.springframework.stereotype.Service

// 메타 조회 전용 서비스. 4단원에서 단계적으로 캐시를 얹어가는 자리.
// 4-0 단계에서는 캐시 없이 매 호출이 DB 직격. 이게 베이스라인.
@Service
class CouponQueryService(
    private val couponRepository: CouponRepository,
    private val cacheMetrics: CacheMetrics,
) {

    fun findMeta(id: Long): CouponMetaResponse {
        cacheMetrics.incrementMetaDbRead()
        val coupon = couponRepository.findById(id)
            .orElseThrow { CouponNotFoundException() }
        return CouponMetaResponse.from(coupon)
    }
}
