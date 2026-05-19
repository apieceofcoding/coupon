package com.apiece.coupon.infrastructure.messaging

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
class KafkaTopicConfig {

    @Bean
    fun issuanceRequestedTopic(): NewTopic =
        TopicBuilder.name(IssuanceTopics.REQUESTED).partitions(3).replicas(1).build()

    // DLT 도 명시적으로 선언 (DeadLetterPublishingRecoverer 가 자동 생성도 하지만
    // 운영 환경에서는 토픽 정책을 명시하는 편이 안전).
    @Bean
    fun issuanceRequestedDltTopic(): NewTopic =
        TopicBuilder.name(IssuanceTopics.REQUESTED_DLT).partitions(3).replicas(1).build()
}
