package com.apiece.coupon.infrastructure.messaging

import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

// Kafka 가 영속/분산/재처리를 처리해주고, 우리는 메시지 한 건 처리만 책임진다.
// at-least-once 라 같은 메시지가 두 번 와도 IssuanceWriter 의 멱등 처리가 받아낸다.
@Component
class IssuanceWorker(
    private val writer: IssuanceWriter,
) {
    @KafkaListener(
        topics = [IssuanceTopics.REQUESTED],
        groupId = IssuanceTopics.CONSUMER_GROUP,
    )
    fun consume(event: IssuanceRequested) {
        writer.write(event)
    }
}
