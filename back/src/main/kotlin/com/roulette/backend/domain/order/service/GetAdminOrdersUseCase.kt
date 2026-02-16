package com.roulette.backend.domain.order.service

import com.roulette.backend.domain.order.domain.Order
import com.roulette.backend.domain.order.domain.OrderStatus
import com.roulette.backend.domain.order.repository.OrderReadRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class GetAdminOrdersUseCase(
    private val orderReadRepository: OrderReadRepository,
) {
    @Transactional(readOnly = true)
    fun execute(status: OrderStatus? = null): AdminOrderListResult {
        val orders = if (status == null) {
            orderReadRepository.findAllByOrderByOrderedAtDesc()
        } else {
            orderReadRepository.findAllByStatusOrderByOrderedAtDesc(status)
        }
        val items = orders.map { order -> order.toAdminItemResult() }
        return AdminOrderListResult(totalItems = items.size, items = items)
    }
}

data class AdminOrderListResult(
    val totalItems: Int,
    val items: List<AdminOrderItemResult>,
)

data class AdminOrderItemResult(
    val orderId: Long,
    val userId: Long,
    val productId: Long,
    val quantity: Int,
    val usedPoints: Int,
    val status: OrderStatus,
    val orderedAt: LocalDateTime,
    val canceledAt: LocalDateTime?,
)

internal fun Order.toAdminItemResult(): AdminOrderItemResult {
    return AdminOrderItemResult(
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
