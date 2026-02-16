package com.roulette.backend.domain.product.controller

import com.roulette.backend.common.response.ApiResponse
import com.roulette.backend.common.util.RequestHeaderContextResolver
import com.roulette.backend.domain.product.dto.ProductListResponse
import com.roulette.backend.domain.product.dto.toResponse
import com.roulette.backend.domain.product.service.GetProductsUseCase
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/products")
class ProductController(
    private val getProductsUseCase: GetProductsUseCase,
    private val requestHeaderContextResolver: RequestHeaderContextResolver,
) {
    @GetMapping
    fun getProducts(
        request: HttpServletRequest,
        @RequestParam(required = false, defaultValue = "true") activeOnly: Boolean,
    ): ApiResponse<ProductListResponse> {
        requestHeaderContextResolver.resolveUserId(request)
        val result = getProductsUseCase.execute(activeOnly = activeOnly)
        return ApiResponse(result.toResponse())
    }
}
