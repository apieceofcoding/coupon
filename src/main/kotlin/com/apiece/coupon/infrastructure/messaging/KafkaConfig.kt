package com.apiece.coupon.infrastructure.messaging

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import tools.jackson.databind.json.JsonMapper
import org.springframework.kafka.support.JacksonMapperUtils
import org.apache.kafka.clients.admin.AdminClientConfig
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaAdmin
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer
import org.springframework.kafka.support.serializer.JacksonJsonSerializer

// Spring Boot 4 부터 KafkaAutoConfiguration 이 빠져서 ProducerFactory / ConsumerFactory /
// KafkaTemplate / Listener Container Factory 를 직접 선언한다.
@Configuration
@EnableKafka
class KafkaConfig(
    @Value("\${spring.kafka.bootstrap-servers}") private val bootstrapServers: String,
) {

    // Spring Kafka 4 권장: JacksonMapperUtils.enhancedJsonMapper() 가 Kafka 용으로
    // 튜닝된 JsonMapper. Jackson 3 의 ServiceLoader 가 classpath 의 KotlinModule,
    // JavaTimeModule 등을 자동 등록해서 별도 registerModule 호출이 필요 없다.
    private val jsonMapper: JsonMapper = JacksonMapperUtils.enhancedJsonMapper()

    // 토픽 자동 생성/파티션 증감은 운영 사고로 직결되므로 KafkaAdmin 의 부팅 시
    // 자동 적용을 끈다. NewTopic 빈은 스펙 문서로만 남고, 실제 토픽 생성/변경은
    // 운영자가 admin 도구나 IaC 로 수동 수행한다.
    @Bean
    fun kafkaAdmin(): KafkaAdmin {
        val admin = KafkaAdmin(mapOf(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers))
        admin.setAutoCreate(false)
        return admin
    }

    @Bean
    fun producerFactory(): ProducerFactory<String, Any> {
        val props = mapOf<String, Any>(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.ACKS_CONFIG to "1",
        )
        return DefaultKafkaProducerFactory(props, StringSerializer(), JacksonJsonSerializer<Any>(jsonMapper))
    }

    @Bean
    fun kafkaTemplate(producerFactory: ProducerFactory<String, Any>): KafkaTemplate<String, Any> =
        KafkaTemplate(producerFactory)

    @Bean
    fun consumerFactory(): ConsumerFactory<String, Any> {
        val props = mapOf<String, Any>(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to IssuanceTopics.CONSUMER_GROUP,
            // "earliest" 는 부하 테스트/로컬 편의용. 운영에서는 "latest" 또는 "none" 으로 바꿔야
            // group.id 오타나 장기 다운 후 재기동 시 전체 메시지 리플레이 사고를 막을 수 있다.
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
        )
        val jsonDelegate = JacksonJsonDeserializer(IssuanceRequested::class.java, jsonMapper).apply {
            addTrustedPackages("com.apiece.coupon.infrastructure.messaging")
        }
        // ErrorHandlingDeserializer 로 감싸야 깨진 payload 가 DLT 로 정상 격리된다.
        @Suppress("UNCHECKED_CAST")
        val valueDeserializer = ErrorHandlingDeserializer(jsonDelegate) as ErrorHandlingDeserializer<Any>
        return DefaultKafkaConsumerFactory(props, StringDeserializer(), valueDeserializer)
    }

    @Bean
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, Any>,
        errorHandler: DefaultErrorHandler,
    ): ConcurrentKafkaListenerContainerFactory<String, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        factory.setConsumerFactory(consumerFactory)
        factory.setCommonErrorHandler(errorHandler)
        return factory
    }
}
