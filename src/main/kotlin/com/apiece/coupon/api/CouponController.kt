package com.apiece.coupon.api

import com.apiece.coupon.api.dto.CouponMetaResponse
import com.apiece.coupon.api.dto.CouponResponse
import com.apiece.coupon.api.dto.CreateCouponRequest
import com.apiece.coupon.api.dto.IssuanceResponse
import com.apiece.coupon.application.CouponQueryService
import com.apiece.coupon.application.CouponService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/coupons")
class CouponController(
    private val couponService: CouponService,
    private val couponQueryService: CouponQueryService,
) {

    @PostMapping
    fun create(@RequestBody request: CreateCouponRequest): ResponseEntity<CouponResponse> {
        val coupon = couponService.createCoupon(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(CouponResponse.from(coupon))
    }

    // 이벤트 페이지 진입 시 호출되는 메타 조회. 모든 사용자가 같은 행을 본다.
    // 4단원에서 캐시 단계가 들어가는 자리. 현재 (4-0) 는 매번 DB 직격이다.
    @GetMapping("/{couponId}")
    fun findMeta(@PathVariable couponId: Long): CouponMetaResponse =
        couponQueryService.findMeta(couponId)

    @PostMapping("/{couponId}/issue")
    fun issue(
        @PathVariable couponId: Long,
        @RequestHeader("X-User-Id") userId: Long,
    ): IssuanceResponse {
        val issuance = couponService.issue(couponId, userId)
        return IssuanceResponse.from(issuance)
    }
}
