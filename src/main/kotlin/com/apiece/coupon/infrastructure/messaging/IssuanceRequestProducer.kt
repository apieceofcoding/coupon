package com.apiece.coupon.infrastructure.messaging

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

// 같은 couponId 가 같은 partition 으로 라우팅되도록 key 는 couponId.
// 같은 쿠폰의 발급 이벤트 순서가 partition 안에서는 보장된다.
@Component
class IssuanceRequestProducer(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
) {
    fun publish(event: IssuanceRequested) {
        kafkaTemplate.send(IssuanceTopics.REQUESTED, event.couponId.toString(), event)
    }
}
