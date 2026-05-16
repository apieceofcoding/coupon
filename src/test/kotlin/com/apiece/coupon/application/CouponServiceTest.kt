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
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class CouponServiceTest {

    private val couponRepository = mockk<CouponRepository>(relaxUnitFun = true)
    private val issuanceRepository = mockk<IssuanceRepository>()
    private val couponIssuer = mockk<CouponIssuer>(relaxUnitFun = true)
    private val service = CouponService(couponRepository, issuanceRepository, couponIssuer)

    @Test
    fun `행사 생성하면 Redis 재고 키 초기화`() {
        val saved = coupon(id = 7L, totalQuantity = 5000)
        every { couponRepository.save(any()) } returns saved

        val result = service.createCoupon(CreateCouponRequest("Flash Event", 5000, 7))

        assertEquals(7L, result.id)
        verify { couponIssuer.initStock(7L, 5000) }
    }

    @Test
    fun `발급 성공시 issued_quantity 증가 + Issuance 저장`() {
        val coupon = coupon(id = 1L, totalQuantity = 10, issuedQuantity = 5)
        val captured = slot<Issuance>()
        every { couponRepository.findById(1L) } returns Optional.of(coupon)
        every { couponRepository.incrementIssuedQuantity(1L) } returns 1
        every { issuanceRepository.save(capture(captured)) } answers { captured.captured.also { it.id = 99L } }

        val result = service.issue(1L, 42L)

        verify { couponIssuer.tryIssue(1L, 42L) }
        verify { couponRepository.incrementIssuedQuantity(1L) }
        assertEquals(42L, result.userId)
        assertEquals(1L, result.couponId)
        assertNotNull(result.expiresAt)
    }

    @Test
    fun `행사 없으면 CouponNotFoundException`() {
        every { couponRepository.findById(99L) } returns Optional.empty()
        assertFailsWith<CouponNotFoundException> { service.issue(99L, 42L) }
    }

    @Test
    fun `시작 시각 전이면 NotStartedException`() {
        val future = LocalDateTime.now().plusDays(1)
        every { couponRepository.findById(1L) } returns Optional.of(coupon(id = 1L, startsAt = future))
        assertFailsWith<NotStartedException> { service.issue(1L, 42L) }
    }

    @Test
    fun `Issuer 가 SoldOutException 을 던지면 그대로 전파`() {
        every { couponRepository.findById(1L) } returns Optional.of(coupon(id = 1L))
        every { couponIssuer.tryIssue(1L, 42L) } throws SoldOutException()
        assertFailsWith<SoldOutException> { service.issue(1L, 42L) }
    }

    @Test
    fun `Issuer 가 AlreadyIssuedException 을 던지면 그대로 전파`() {
        every { couponRepository.findById(1L) } returns Optional.of(coupon(id = 1L))
        every { couponIssuer.tryIssue(1L, 42L) } throws AlreadyIssuedException()
        assertFailsWith<AlreadyIssuedException> { service.issue(1L, 42L) }
    }

    @Test
    fun `Issuer 가 통과해도 DB UNIQUE 가 막으면 AlreadyIssuedException`() {
        every { couponRepository.findById(1L) } returns Optional.of(coupon(id = 1L))
        every { couponRepository.incrementIssuedQuantity(1L) } returns 1
        every { issuanceRepository.save(any()) } throws DataIntegrityViolationException("uk_issuance_user_coupon")

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
