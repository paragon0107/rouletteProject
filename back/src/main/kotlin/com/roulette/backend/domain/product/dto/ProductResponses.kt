package com.roulette.backend.domain.product.dto

import com.roulette.backend.domain.product.domain.ProductStatus
import com.roulette.backend.domain.product.service.ProductItemResult
import com.roulette.backend.domain.product.service.ProductListResult
import java.time.LocalDateTime

data class ProductListResponse(
    val totalItems: Int,
    val items: List<ProductResponse>,
)

data class ProductResponse(
    val productId: Long,
    val name: String,
    val description: String,
    val pricePoints: Int,
    val stock: Int,
    val status: ProductStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

fun ProductListResult.toResponse(): ProductListResponse {
    return ProductListResponse(
        totalItems = totalItems,
        items = items.map { it.toResponse() },
    )
}

fun ProductItemResult.toResponse(): ProductResponse {
    return ProductResponse(
        productId = productId,
        name = name,
        description = description,
        pricePoints = pricePoints,
        stock = stock,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
