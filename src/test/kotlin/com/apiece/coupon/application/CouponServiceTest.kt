package com.apiece.coupon.application

import com.apiece.coupon.api.dto.CreateCouponRequest
import com.apiece.coupon.domain.Coupon
import com.apiece.coupon.domain.CouponRepository
import com.apiece.coupon.domain.Issuance
import com.apiece.coupon.domain.IssuanceRepository
import com.apiece.coupon.support.AlreadyIssuedException
import com.apiece.coupon.support.CouponNotFoundException
import com.apiece.coupon.support.NotStartedException
import com.apiece.coupon.support.SoldOutException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class CouponServiceTest {

    private val couponRepository = mockk<CouponRepository>()
    private val issuanceRepository = mockk<IssuanceRepository>()
    private val service = CouponService(couponRepository, issuanceRepository)

    @Test
    fun `행사 생성하면 저장된 Coupon 반환`() {
        val saved = coupon(id = 1L, totalQuantity = 100)
        every { couponRepository.save(any()) } returns saved

        val result = service.createCoupon(CreateCouponRequest("5월 행사", 100, 7))

        assertEquals(1L, result.id)
        assertEquals(100, result.totalQuantity)
    }

    @Test
    fun `발급 성공시 issuedQuantity 증가 + Issuance 저장`() {
        val coupon = coupon(id = 1L, totalQuantity = 10, issuedQuantity = 5)
        val captured = slot<Issuance>()
        every { couponRepository.findByIdForUpdate(1L) } returns coupon
        every { issuanceRepository.existsByUserIdAndCouponId(42L, 1L) } returns false
        every { issuanceRepository.save(capture(captured)) } answers { captured.captured.also { it.id = 99L } }

        val result = service.issue(1L, 42L)

        assertEquals(6, coupon.issuedQuantity)
        assertEquals(42L, result.userId)
        assertEquals(1L, result.couponId)
        assertNotNull(result.expiresAt)
    }

    @Test
    fun `행사 없으면 CouponNotFoundException`() {
        every { couponRepository.findByIdForUpdate(99L) } returns null
        assertFailsWith<CouponNotFoundException> { service.issue(99L, 42L) }
    }

    @Test
    fun `시작 시각 전이면 NotStartedException`() {
        val future = LocalDateTime.now().plusDays(1)
        every { couponRepository.findByIdForUpdate(1L) } returns coupon(id = 1L, startsAt = future)
        assertFailsWith<NotStartedException> { service.issue(1L, 42L) }
    }

    @Test
    fun `매진이면 SoldOutException`() {
        every { couponRepository.findByIdForUpdate(1L) } returns coupon(id = 1L, totalQuantity = 5, issuedQuantity = 5)
        assertFailsWith<SoldOutException> { service.issue(1L, 42L) }
    }

    @Test
    fun `이미 발급된 사용자면 AlreadyIssuedException`() {
        every { couponRepository.findByIdForUpdate(1L) } returns coupon(id = 1L)
        every { issuanceRepository.existsByUserIdAndCouponId(42L, 1L) } returns true
        assertFailsWith<AlreadyIssuedException> { service.issue(1L, 42L) }
    }

    private fun coupon(
        id: Long? = null,
        totalQuantity: Int = 100,
        issuedQuantity: Int = 0,
        startsAt: LocalDateTime? = null,
        validityDays: Int = 7,
    ) = Coupon(
        name = "테스트",
        totalQuantity = totalQuantity,
        validityDays = validityDays,
        startsAt = startsAt,
        issuedQuantity = issuedQuantity,
        id = id,
    )
}
