package com.apiece.coupon.infrastructure.messaging

import org.apache.kafka.common.TopicPartition
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff

// poison pill (깨진 payload, 영구 실패) 가 한 건 끼면 Consumer 가 그 자리에서 멈추고
// 뒤 메시지가 다 막힌다. N번 재시도 후 DLT (Dead Letter Topic) 로 격리해
// 메인 Consumer 는 다음 메시지로 진행할 수 있게 한다.
@Configuration
class KafkaErrorHandlerConfig {

    @Bean
    fun errorHandler(template: KafkaTemplate<String, Any>): DefaultErrorHandler {
        // 실패 시 원본 토픽명 + ".DLT" 토픽으로 메시지를 보낸다. partition 도 보존.
        val recoverer = DeadLetterPublishingRecoverer(template) { record, _ ->
            TopicPartition("${record.topic()}.DLT", record.partition())
        }
        // 1초 간격으로 최대 3번 재시도, 그래도 실패면 DLT 로 보내고 다음 메시지로 진행.
        return DefaultErrorHandler(recoverer, FixedBackOff(1_000L, 3L))
    }
}
