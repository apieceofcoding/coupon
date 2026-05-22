package com.apiece.coupon.infrastructure.messaging

import com.apiece.coupon.domain.Issuance
import com.apiece.coupon.domain.IssuanceRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class IssuanceWriter(
    private val issuanceRepository: IssuanceRepository,
) {
    fun write(event: IssuanceRequested) {
        try {
            issuanceRepository.save(
                Issuance(
                    userId = event.userId,
                    couponId = event.couponId,
                    issuedAt = event.issuedAt,
                    expiresAt = event.expiresAt,
                )
            )
        } catch (e: DataIntegrityViolationException) {
            log.debug { "UNIQUE 위반은 멱등 처리: couponId=${event.couponId}, userId=${event.userId}" }
        }
    }
}
