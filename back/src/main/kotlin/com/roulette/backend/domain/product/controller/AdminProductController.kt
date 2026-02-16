package com.roulette.backend.domain.product.controller

import com.roulette.backend.common.response.ApiResponse
import com.roulette.backend.common.util.RequestHeaderContextResolver
import com.roulette.backend.domain.product.dto.CreateProductRequest
import com.roulette.backend.domain.product.dto.ProductListResponse
import com.roulette.backend.domain.product.dto.ProductResponse
import com.roulette.backend.domain.product.dto.UpdateProductRequest
import com.roulette.backend.domain.product.dto.toResponse
import com.roulette.backend.domain.product.service.CreateProductCommand
import com.roulette.backend.domain.product.service.CreateProductUseCase
import com.roulette.backend.domain.product.service.DeleteProductUseCase
import com.roulette.backend.domain.product.service.GetProductsUseCase
import com.roulette.backend.domain.product.service.UpdateProductCommand
import com.roulette.backend.domain.product.service.UpdateProductUseCase
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/products")
class AdminProductController(
    private val getProductsUseCase: GetProductsUseCase,
    private val createProductUseCase: CreateProductUseCase,
    private val updateProductUseCase: UpdateProductUseCase,
    private val deleteProductUseCase: DeleteProductUseCase,
    private val requestHeaderContextResolver: RequestHeaderContextResolver,
) {
    @GetMapping
    fun getProducts(request: HttpServletRequest): ApiResponse<ProductListResponse> {
        requestHeaderContextResolver.requireAdmin(request)
        val result = getProductsUseCase.execute(activeOnly = false)
        return ApiResponse(result.toResponse())
    }

    @PostMapping
    fun createProduct(
        request: HttpServletRequest,
        @Valid @RequestBody body: CreateProductRequest,
    ): ApiResponse<ProductResponse> {
        requestHeaderContextResolver.requireAdmin(request)
        val result = createProductUseCase.execute(
            CreateProductCommand(
                name = body.name,
                description = body.description,
                pricePoints = body.pricePoints,
                stock = body.stock,
                status = body.status,
            ),
        )
        return ApiResponse(result.toResponse())
    }

    @PatchMapping("/{productId}")
    fun updateProduct(
        request: HttpServletRequest,
        @PathVariable productId: Long,
        @Valid @RequestBody body: UpdateProductRequest,
    ): ApiResponse<ProductResponse> {
        requestHeaderContextResolver.requireAdmin(request)
        val result = updateProductUseCase.execute(
            UpdateProductCommand(
                productId = productId,
                name = body.name,
                description = body.description,
                pricePoints = body.pricePoints,
                stock = body.stock,
                status = body.status,
            ),
        )
        return ApiResponse(result.toResponse())
    }

    @DeleteMapping("/{productId}")
    fun deleteProduct(
        request: HttpServletRequest,
        @PathVariable productId: Long,
    ): ApiResponse<Unit> {
        requestHeaderContextResolver.requireAdmin(request)
        deleteProductUseCase.execute(productId)
        return ApiResponse(Unit)
    }
}
