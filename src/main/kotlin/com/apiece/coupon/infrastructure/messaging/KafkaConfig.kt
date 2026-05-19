package com.apiece.coupon.infrastructure.messaging

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
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
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer

// Spring Boot 4 부터 KafkaAutoConfiguration 이 빠져서 ProducerFactory / ConsumerFactory /
// KafkaTemplate / Listener Container Factory 를 직접 선언한다.
@Configuration
@EnableKafka
class KafkaConfig(
    @Value("\${spring.kafka.bootstrap-servers}") private val bootstrapServers: String,
) {

    // JavaTimeModule (LocalDateTime) + KotlinModule (val 생성자) 가 등록된 ObjectMapper.
    // spring-kafka 의 기본 ObjectMapper 는 모듈 자동 검색을 하지 않아서 직접 주입한다.
    private val objectMapper: ObjectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerModule(KotlinModule.Builder().build())

    @Bean
    fun kafkaAdmin(): KafkaAdmin =
        KafkaAdmin(mapOf(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers))

    @Bean
    fun producerFactory(): ProducerFactory<String, Any> {
        val props = mapOf<String, Any>(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.ACKS_CONFIG to "1",
        )
        return DefaultKafkaProducerFactory(props, StringSerializer(), JsonSerializer<Any>(objectMapper))
    }

    @Bean
    fun kafkaTemplate(producerFactory: ProducerFactory<String, Any>): KafkaTemplate<String, Any> =
        KafkaTemplate(producerFactory)

    @Bean
    fun consumerFactory(): ConsumerFactory<String, Any> {
        val props = mapOf<String, Any>(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to IssuanceTopics.CONSUMER_GROUP,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
        )
        val jsonDelegate = JsonDeserializer(IssuanceRequested::class.java, objectMapper).apply {
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
