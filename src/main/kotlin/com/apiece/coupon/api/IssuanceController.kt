package com.apiece.coupon.api

import com.apiece.coupon.api.dto.IssuanceResponse
import com.apiece.coupon.application.IssuanceService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/issuances")
class IssuanceController(
    private val issuanceService: IssuanceService,
) {

    @PostMapping("/{issuanceId}/use")
    fun use(
        @PathVariable issuanceId: Long,
        @RequestHeader("X-User-Id") userId: Long,
    ): IssuanceResponse {
        val issuance = issuanceService.use(issuanceId, userId)
        return IssuanceResponse.from(issuance)
    }
}
