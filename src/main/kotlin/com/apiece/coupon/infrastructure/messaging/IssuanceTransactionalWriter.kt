package com.apiece.coupon.infrastructure.messaging

import com.apiece.coupon.domain.CouponRepository
import com.apiece.coupon.domain.Issuance
import com.apiece.coupon.domain.IssuanceRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

// 트랜잭션 경계 전용. catch 를 안 두어 UNIQUE 위반 같은 예외가 메서드 밖으로 그대로
// escape 되도록 한다. 그래야 Spring 이 자동 rollback 하고, 같은 클래스 안에서 catch
// 했을 때 발생하는 UnexpectedRollbackException 함정을 피할 수 있다. 멱등 처리는
// IssuanceWriter (호출자) 에서.
@Component
class IssuanceTransactionalWriter(
    private val issuanceRepository: IssuanceRepository,
    private val couponRepository: CouponRepository,
) {
    @Transactional
    fun insertAndIncrement(event: IssuanceRequested) {
        issuanceRepository.save(
            Issuance(
                userId = event.userId,
                couponId = event.couponId,
                issuedAt = event.issuedAt,
                expiresAt = event.expiresAt,
            )
        )
        couponRepository.incrementIssuedQuantity(event.couponId)
    }
}
