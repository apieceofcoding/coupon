package com.apiece.coupon.infrastructure.messaging

import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

// 4.1.a 의 InMemoryIssuanceQueue + InMemoryIssuanceWorker 가 한 메서드로 줄어든 형태.
// @Async 가 메서드를 ThreadPoolTaskExecutor 로 위임하고, 그 안의 LinkedBlockingQueue 가 큐 역할.
// 본질은 4.1.a 와 동일: JVM 죽으면 work queue 안 메시지 손실, 인스턴스마다 따로 돎.
@Component
class IssuanceEventHandler(
    private val writer: IssuanceWriter,
) {
    @Async
    @EventListener
    fun handle(event: IssuanceRequested) {
        writer.write(event)
    }
}
