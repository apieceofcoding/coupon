package com.apiece.coupon.application

import com.apiece.coupon.domain.Issuance
import com.apiece.coupon.domain.IssuanceRepository
import com.apiece.coupon.domain.IssuanceStatus
import com.apiece.coupon.support.AlreadyUsedException
import com.apiece.coupon.support.ExpiredException
import com.apiece.coupon.support.IssuanceNotFoundException
import com.apiece.coupon.support.NotOwnerException
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class IssuanceServiceTest {

    private val issuanceRepository = mockk<IssuanceRepository>()
    private val service = IssuanceService(issuanceRepository)

    @Test
    fun `사용 성공시 status USED + usedAt 기록`() {
        val issuance = issuance(id = 1L, userId = 42L, expiresIn = 1)
        every { issuanceRepository.findById(1L) } returns Optional.of(issuance)

        val result = service.use(1L, 42L)

        assertEquals(IssuanceStatus.USED, result.status)
        assertNotNull(result.usedAt)
    }

    @Test
    fun `발급내역 없으면 IssuanceNotFoundException`() {
        every { issuanceRepository.findById(99L) } returns Optional.empty()
        assertFailsWith<IssuanceNotFoundException> { service.use(99L, 42L) }
    }

    @Test
    fun `본인 아니면 NotOwnerException`() {
        every { issuanceRepository.findById(1L) } returns Optional.of(issuance(id = 1L, userId = 42L))
        assertFailsWith<NotOwnerException> { service.use(1L, 999L) }
    }

    @Test
    fun `이미 사용된 쿠폰이면 AlreadyUsedException`() {
        val used = issuance(id = 1L, userId = 42L).apply { status = IssuanceStatus.USED }
        every { issuanceRepository.findById(1L) } returns Optional.of(used)
        assertFailsWith<AlreadyUsedException> { service.use(1L, 42L) }
    }

    @Test
    fun `만료된 쿠폰이면 ExpiredException (lazy 판단)`() {
        val expired = issuance(id = 1L, userId = 42L, expiresIn = -1) // 이미 만료
        every { issuanceRepository.findById(1L) } returns Optional.of(expired)
        assertFailsWith<ExpiredException> { service.use(1L, 42L) }
    }

    @Test
    fun `findByUser는 발급 시각 역순 리스트 반환`() {
        val list = listOf(issuance(id = 2L, userId = 42L), issuance(id = 1L, userId = 42L))
        every { issuanceRepository.findByUserIdOrderByIssuedAtDesc(42L) } returns list
        assertEquals(2, service.findByUser(42L).size)
    }

    private fun issuance(
        id: Long? = null,
        userId: Long = 42L,
        couponId: Long = 1L,
        expiresIn: Long = 1,
    ): Issuance {
        val now = LocalDateTime.now()
        return Issuance(
            userId = userId,
            couponId = couponId,
            issuedAt = now,
            expiresAt = now.plusDays(expiresIn),
            id = id,
        )
    }
}
