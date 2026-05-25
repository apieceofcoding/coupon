package com.apiece.coupon.infrastructure.cache

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "coupon.sold-out")
class SoldOutProperties(
    val ttlSeconds: Long,
    val fastPathTtlMs: Long,
)
