package com.apiece.coupon.api.dto

import com.apiece.coupon.application.CacheMetricsSnapshot

// 4단원 부하 테스트에서 단계별 효과를 비교할 때 외부에서 카운터 값을 끌어가는 응답.
class CacheMetricsResponse(
    val couponDbReads: Long,
    val couponCacheHits: Long,
    val soldOutRedisExists: Long,
    val soldOutFastPathHits: Long,
) {
    companion object {
        fun from(snapshot: CacheMetricsSnapshot): CacheMetricsResponse = CacheMetricsResponse(
            couponDbReads = snapshot.couponDbReads,
            couponCacheHits = snapshot.couponCacheHits,
            soldOutRedisExists = snapshot.soldOutRedisExists,
            soldOutFastPathHits = snapshot.soldOutFastPathHits,
        )
    }
}
