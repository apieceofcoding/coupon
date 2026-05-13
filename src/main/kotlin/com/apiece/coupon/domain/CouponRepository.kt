package com.apiece.coupon.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CouponRepository : JpaRepository<Coupon, Long> {

    @Modifying
    @Query("UPDATE Coupon c SET c.issuedQuantity = c.issuedQuantity + 1 WHERE c.id = :id")
    fun incrementIssuedQuantity(@Param("id") id: Long): Int
}
