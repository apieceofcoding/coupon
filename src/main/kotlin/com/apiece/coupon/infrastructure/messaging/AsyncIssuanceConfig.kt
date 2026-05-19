package com.apiece.coupon.infrastructure.messaging

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

// v2a 와 동일 조건으로 비교하기 위해 worker 1개 + queue 10_000.
// 차이점은 "직접 thread {} 와 LinkedBlockingQueue 를 만들었나, Spring 이 만들어줬나" 뿐.
@Configuration
@EnableAsync
class AsyncIssuanceConfig {

    @Bean(name = ["taskExecutor"])
    fun issuanceTaskExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 1
        executor.maxPoolSize = 1
        executor.queueCapacity = 10_000
        executor.setThreadNamePrefix("issuance-async-")
        executor.setWaitForTasksToCompleteOnShutdown(true)
        executor.setAwaitTerminationSeconds(30)
        executor.initialize()
        return executor
    }
}
