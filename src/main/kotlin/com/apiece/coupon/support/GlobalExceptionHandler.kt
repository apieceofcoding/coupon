package com.apiece.coupon.support

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(DomainException::class)
    fun handleDomain(
        ex: DomainException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetail.forStatusAndDetail(ex.httpStatus, ex.message ?: "")
        pd.title = humanize(ex.code)
        pd.instance = URI.create(request.requestURI)
        pd.setProperty("code", ex.code)
        return ResponseEntity.status(ex.httpStatus).body(pd)
    }

    private fun humanize(code: String): String =
        code.split('_').joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.titlecase() } }
}
