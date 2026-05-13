package com.apiece.coupon.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "coupon")
class Coupon(
    @Column(nullable = false, length = 80)
    var name: String,

    @Column(name = "total_quantity", nullable = false)
    var totalQuantity: Int,

    @Column(name = "validity_days", nullable = false)
    var validityDays: Int = 7,

    @Column(name = "starts_at")
    var startsAt: LocalDateTime? = null,

    @Column(name = "issued_quantity", nullable = false)
    var issuedQuantity: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
) {
    fun isBookingOpen(now: LocalDateTime): Boolean =
        startsAt?.let { !now.isBefore(it) } ?: true
}
