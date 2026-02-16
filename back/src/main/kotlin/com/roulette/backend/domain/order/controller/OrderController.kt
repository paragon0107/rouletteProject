package com.roulette.backend.domain.order.controller

import com.roulette.backend.common.response.ApiResponse
import com.roulette.backend.common.util.RequestHeaderContextResolver
import com.roulette.backend.domain.order.domain.OrderStatus
import com.roulette.backend.domain.order.dto.CreateOrderRequest
import com.roulette.backend.domain.order.dto.OrderListResponse
import com.roulette.backend.domain.order.dto.OrderResponse
import com.roulette.backend.domain.order.dto.toResponse
import com.roulette.backend.domain.order.service.CreateOrderCommand
import com.roulette.backend.domain.order.service.CreateOrderUseCase
import com.roulette.backend.domain.order.service.GetMyOrdersUseCase
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val createOrderUseCase: CreateOrderUseCase,
    private val getMyOrdersUseCase: GetMyOrdersUseCase,
    private val requestHeaderContextResolver: RequestHeaderContextResolver,
) {
    @PostMapping
    fun createOrder(
        request: HttpServletRequest,
        @Valid @RequestBody body: CreateOrderRequest,
    ): ApiResponse<OrderResponse> {
        val userId = requestHeaderContextResolver.resolveUserId(request)
        val result = createOrderUseCase.execute(
            CreateOrderCommand(
                userId = userId,
                productId = body.productId,
                quantity = body.quantity,
            ),
        )
        return ApiResponse(result.toResponse())
    }

    @GetMapping("/me")
    fun getMyOrders(
        request: HttpServletRequest,
        @RequestParam(required = false) status: OrderStatus?,
    ): ApiResponse<OrderListResponse> {
        val userId = requestHeaderContextResolver.resolveUserId(request)
        val result = getMyOrdersUseCase.execute(userId = userId, status = status)
        return ApiResponse(result.toResponse())
    }
}
