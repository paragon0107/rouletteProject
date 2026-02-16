package com.roulette.backend.domain.order.repository

import com.roulette.backend.domain.order.domain.Order
import com.roulette.backend.domain.order.domain.OrderStatus
import org.springframework.data.jpa.repository.JpaRepository

interface OrderReadRepository : JpaRepository<Order, Long> {
    fun findAllByOrderByOrderedAtDesc(): List<Order>

    fun findAllByUserIdOrderByOrderedAtDesc(userId: Long): List<Order>

    fun findAllByStatusOrderByOrderedAtDesc(status: OrderStatus): List<Order>

    fun findAllByUserIdAndStatusOrderByOrderedAtDesc(
        userId: Long,
        status: OrderStatus,
    ): List<Order>
}
