package com.roulette.backend.domain.order.controller

import com.roulette.backend.common.response.ApiResponse
import com.roulette.backend.common.util.RequestHeaderContextResolver
import com.roulette.backend.domain.order.domain.OrderStatus
import com.roulette.backend.domain.order.dto.CancelOrderResponse
import com.roulette.backend.domain.order.dto.OrderListItemResponse
import com.roulette.backend.domain.order.dto.OrderListResponse
import com.roulette.backend.domain.order.dto.UpdateOrderStatusRequest
import com.roulette.backend.domain.order.dto.toResponse
import com.roulette.backend.domain.order.service.CancelOrderUseCase
import com.roulette.backend.domain.order.service.GetAdminOrdersUseCase
import com.roulette.backend.domain.order.service.UpdateOrderStatusUseCase
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/orders")
class AdminOrderController(
    private val getAdminOrdersUseCase: GetAdminOrdersUseCase,
    private val updateOrderStatusUseCase: UpdateOrderStatusUseCase,
    private val cancelOrderUseCase: CancelOrderUseCase,
    private val requestHeaderContextResolver: RequestHeaderContextResolver,
) {
    @GetMapping
    fun getOrders(
        request: HttpServletRequest,
        @RequestParam(required = false) status: OrderStatus?,
    ): ApiResponse<OrderListResponse> {
        requestHeaderContextResolver.requireAdmin(request)
        val result = getAdminOrdersUseCase.execute(status)
        return ApiResponse(result.toResponse())
    }

    @PatchMapping("/{orderId}/status")
    fun updateOrderStatus(
        request: HttpServletRequest,
        @PathVariable orderId: Long,
        @Valid @RequestBody body: UpdateOrderStatusRequest,
    ): ApiResponse<OrderListItemResponse> {
        requestHeaderContextResolver.requireAdmin(request)
        val result = updateOrderStatusUseCase.execute(orderId, body.status)
        return ApiResponse(result.toResponse())
    }

    @PostMapping("/{orderId}/cancel")
    fun cancelOrder(
        request: HttpServletRequest,
        @PathVariable orderId: Long,
    ): ApiResponse<CancelOrderResponse> {
        requestHeaderContextResolver.requireAdmin(request)
        val result = cancelOrderUseCase.execute(orderId)
        return ApiResponse(result.toResponse())
    }
}
