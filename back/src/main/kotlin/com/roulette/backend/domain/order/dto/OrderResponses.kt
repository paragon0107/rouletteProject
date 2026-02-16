package com.roulette.backend.domain.order.dto

import com.roulette.backend.domain.order.domain.OrderStatus
import com.roulette.backend.domain.order.service.AdminOrderItemResult
import com.roulette.backend.domain.order.service.AdminOrderListResult
import com.roulette.backend.domain.order.service.CancelOrderResult
import com.roulette.backend.domain.order.service.CreateOrderResult
import com.roulette.backend.domain.order.service.MyOrderItemResult
import com.roulette.backend.domain.order.service.MyOrderListResult
import java.time.LocalDateTime

data class OrderResponse(
    val orderId: Long,
    val userId: Long,
    val productId: Long,
    val quantity: Int,
    val usedPoints: Int,
    val status: OrderStatus,
    val orderedAt: LocalDateTime,
)

data class OrderListResponse(
    val totalItems: Int,
    val items: List<OrderListItemResponse>,
)

data class OrderListItemResponse(
    val orderId: Long,
    val userId: Long,
    val productId: Long,
    val quantity: Int,
    val usedPoints: Int,
    val status: OrderStatus,
    val orderedAt: LocalDateTime,
    val canceledAt: LocalDateTime?,
)

data class CancelOrderResponse(
    val orderId: Long,
    val userId: Long,
    val refundedPoints: Int,
    val refundedAt: LocalDateTime,
)

fun CreateOrderResult.toResponse(): OrderResponse {
    return OrderResponse(
        orderId = orderId,
        userId = userId,
        productId = productId,
        quantity = quantity,
        usedPoints = usedPoints,
        status = OrderStatus.PLACED,
        orderedAt = orderedAt,
    )
}

fun MyOrderListResult.toResponse(): OrderListResponse {
    return OrderListResponse(
        totalItems = totalItems,
        items = items.map { it.toResponse() },
    )
}

fun AdminOrderListResult.toResponse(): OrderListResponse {
    return OrderListResponse(
        totalItems = totalItems,
        items = items.map { it.toResponse() },
    )
}

fun MyOrderItemResult.toResponse(): OrderListItemResponse {
    return OrderListItemResponse(
        orderId = orderId,
        userId = userId,
        productId = productId,
        quantity = quantity,
        usedPoints = usedPoints,
        status = status,
        orderedAt = orderedAt,
        canceledAt = canceledAt,
    )
}

fun AdminOrderItemResult.toResponse(): OrderListItemResponse {
    return OrderListItemResponse(
        orderId = orderId,
        userId = userId,
        productId = productId,
        quantity = quantity,
        usedPoints = usedPoints,
        status = status,
        orderedAt = orderedAt,
        canceledAt = canceledAt,
    )
}

fun CancelOrderResult.toResponse(): CancelOrderResponse {
    return CancelOrderResponse(
        orderId = orderId,
        userId = userId,
        refundedPoints = refundedPoints,
        refundedAt = refundedAt,
    )
}
