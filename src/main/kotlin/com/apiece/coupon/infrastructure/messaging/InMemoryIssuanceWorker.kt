package com.apiece.coupon.infrastructure.messaging

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import kotlin.concurrent.thread

@Component
class InMemoryIssuanceWorker(
    private val queue: InMemoryIssuanceQueue,
    private val writer: IssuanceWriter,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private lateinit var workerThread: Thread

    @PostConstruct
    fun start() {
        workerThread = thread(name = "issuance-worker", isDaemon = true) {
            while (!Thread.currentThread().isInterrupted) {
                val event = try {
                    queue.poll() ?: continue
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
                try {
                    writer.write(event)
                } catch (e: Exception) {
                    log.error("Worker write 실패: couponId={}, userId={}", event.couponId, event.userId, e)
                }
            }
            log.info("issuance-worker 종료")
        }
    }

    @PreDestroy
    fun stop() {
        if (::workerThread.isInitialized) {
            workerThread.interrupt()
        }
    }
}
