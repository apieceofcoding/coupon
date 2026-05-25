package com.apiece.coupon.api.dto

import com.apiece.coupon.application.CacheMetricsSnapshot

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
