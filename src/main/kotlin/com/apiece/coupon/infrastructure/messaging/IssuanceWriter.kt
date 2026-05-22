package com.apiece.coupon.infrastructure.messaging

import com.apiece.coupon.domain.CouponRepository
import com.apiece.coupon.domain.Issuance
import com.apiece.coupon.domain.IssuanceRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
class IssuanceWriter(
    private val issuanceRepository: IssuanceRepository,
    private val couponRepository: CouponRepository,
) {
    // 한 트랜잭션 안에서 issuance INSERT + coupon.issued_quantity UPDATE 를 같이 처리.
    // UPDATE 의 "+1" 은 SQL 단에서 원자적이라 동시 갱신끼리 lost update 가 없다.
    // INSERT 가 UNIQUE 위반이면 saveAndFlush 가 즉시 throw → catch → UPDATE 도 실행 안 됨 →
    // 트랜잭션은 rollback 되어 카운터/행 둘 다 변하지 않으니 원자적 멱등 처리.
    @Transactional
    fun write(event: IssuanceRequested) {
        try {
            issuanceRepository.saveAndFlush(
                Issuance(
                    userId = event.userId,
                    couponId = event.couponId,
                    issuedAt = event.issuedAt,
                    expiresAt = event.expiresAt,
                )
            )
            couponRepository.incrementIssuedQuantity(event.couponId)
        } catch (e: DataIntegrityViolationException) {
            log.debug { "UNIQUE 위반은 멱등 처리: couponId=${event.couponId}, userId=${event.userId}" }
        }
    }
}
