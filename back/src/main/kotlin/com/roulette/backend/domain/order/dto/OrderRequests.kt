package com.roulette.backend.domain.order.dto

import com.roulette.backend.domain.order.domain.OrderStatus
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class CreateOrderRequest(
    @field:Min(value = 1, message = "상품 식별자는 1 이상이어야 합니다.")
    val productId: Long,
    @field:Min(value = 1, message = "주문 수량은 1 이상이어야 합니다.")
    @field:Max(value = 20, message = "주문 수량은 20 이하여야 합니다.")
    val quantity: Int,
)

data class UpdateOrderStatusRequest(
    val status: OrderStatus,
)
