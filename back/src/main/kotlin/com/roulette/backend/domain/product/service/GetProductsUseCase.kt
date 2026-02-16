package com.roulette.backend.domain.product.service

import com.roulette.backend.domain.product.domain.Product
import com.roulette.backend.domain.product.domain.ProductStatus
import com.roulette.backend.domain.product.repository.ProductReadRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetProductsUseCase(
    private val productReadRepository: ProductReadRepository,
) {
    @Transactional(readOnly = true)
    fun execute(activeOnly: Boolean = true): ProductListResult {
        val products = if (activeOnly) {
            productReadRepository.findAllByStatus(ProductStatus.ACTIVE)
        } else {
            productReadRepository.findAllByOrderByCreatedAtDesc()
        }
        val items = products.map { product -> product.toItemResult() }
        return ProductListResult(totalItems = items.size, items = items)
    }
}

data class ProductListResult(
    val totalItems: Int,
    val items: List<ProductItemResult>,
)

data class ProductItemResult(
    val productId: Long,
    val name: String,
    val description: String,
    val pricePoints: Int,
    val stock: Int,
    val status: ProductStatus,
    val createdAt: java.time.LocalDateTime,
    val updatedAt: java.time.LocalDateTime,
)

private fun Product.toItemResult(): ProductItemResult {
    return ProductItemResult(
        productId = requireNotNull(id),
        name = name,
        description = description,
        pricePoints = pricePoints,
        stock = stock,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
