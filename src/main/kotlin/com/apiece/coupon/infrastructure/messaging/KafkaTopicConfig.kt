package com.apiece.coupon.infrastructure.messaging

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

// KafkaAdmin.autoCreate=false 로 두었기 때문에 이 NewTopic 빈들은 부팅 시 자동
// 적용되지 않는다. 토픽의 기대 스펙(파티션 수, 복제 수)을 코드에 박아두는 문서
// 역할이며, 실제 생성/변경은 admin 스크립트나 IaC 로 수행한다.
@Configuration
class KafkaTopicConfig {

    @Bean
    fun issuanceRequestedTopic(): NewTopic =
        TopicBuilder.name(IssuanceTopics.REQUESTED).partitions(3).replicas(1).build()

    @Bean
    fun issuanceRequestedDltTopic(): NewTopic =
        TopicBuilder.name(IssuanceTopics.REQUESTED_DLT).partitions(3).replicas(1).build()
}
