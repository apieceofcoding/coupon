package com.apiece.coupon.infrastructure.messaging

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Component
import kotlin.concurrent.thread

private val log = KotlinLogging.logger {}

@Component
class InMemoryIssuanceWorker(
    private val queue: InMemoryIssuanceQueue,
    private val writer: IssuanceWriter,
) {
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
                    log.error(e) { "Worker write 실패: couponId=${event.couponId}, userId=${event.userId}" }
                }
            }
            log.info { "issuance-worker 종료" }
        }
    }

    @PreDestroy
    fun stop() {
        if (::workerThread.isInitialized) {
            workerThread.interrupt()
        }
    }
}
