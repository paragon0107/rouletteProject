package com.roulette.backend.domain.order.service

import com.roulette.backend.domain.order.domain.Order
import com.roulette.backend.domain.order.domain.OrderStatus
import com.roulette.backend.domain.order.repository.OrderReadRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class GetMyOrdersUseCase(
    private val orderReadRepository: OrderReadRepository,
) {
    @Transactional(readOnly = true)
    fun execute(
        userId: Long,
        status: OrderStatus? = null,
    ): MyOrderListResult {
        val orders = if (status == null) {
            orderReadRepository.findAllByUserIdOrderByOrderedAtDesc(userId)
        } else {
            orderReadRepository.findAllByUserIdAndStatusOrderByOrderedAtDesc(userId, status)
        }
        val items = orders.map { order -> order.toItemResult() }
        return MyOrderListResult(totalItems = items.size, items = items)
    }
}

data class MyOrderListResult(
    val totalItems: Int,
    val items: List<MyOrderItemResult>,
)

data class MyOrderItemResult(
    val orderId: Long,
    val userId: Long,
    val productId: Long,
    val quantity: Int,
    val usedPoints: Int,
    val status: OrderStatus,
    val orderedAt: LocalDateTime,
    val canceledAt: LocalDateTime?,
)

private fun Order.toItemResult(): MyOrderItemResult {
    return MyOrderItemResult(
        orderId = requireNotNull(id),
        userId = userId,
        productId = productId,
        quantity = quantity,
        usedPoints = usedPoints,
        status = status,
        orderedAt = orderedAt,
        canceledAt = canceledAt,
    )
}
