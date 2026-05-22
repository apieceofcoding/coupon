package com.apiece.coupon.infrastructure.messaging

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

// 트랜잭션 경계 (IssuanceTransactionalWriter) 의 호출자. catch 가 트랜잭션 밖에 있어야
// Spring 이 자동 rollback 한 뒤 UnexpectedRollbackException 없이 멱등 처리할 수 있다.
@Component
class IssuanceWriter(
    private val transactional: IssuanceTransactionalWriter,
) {
    fun write(event: IssuanceRequested) {
        try {
            transactional.insertAndIncrement(event)
        } catch (e: DataIntegrityViolationException) {
            // INSERT 의 UNIQUE 위반: 같은 (couponId, userId) 가 이미 발급됨.
            // 트랜잭션은 이미 rollback 됐으므로 카운터/행 모두 변하지 않은 상태이며,
            // 우리는 그대로 무시해 at-least-once 의 중복 메시지를 흡수한다.
            log.debug { "UNIQUE 위반은 멱등 처리: couponId=${event.couponId}, userId=${event.userId}" }
        }
    }
}
