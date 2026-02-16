package com.roulette.backend.domain.product.service

import com.roulette.backend.common.exception.BusinessException
import com.roulette.backend.domain.product.domain.ProductStatus
import com.roulette.backend.domain.product.repository.ProductReadRepository
import com.roulette.backend.domain.product.repository.ProductWriteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateProductUseCase(
    private val productWriteRepository: ProductWriteRepository,
    private val productReadRepository: ProductReadRepository,
) {
    @Transactional
    fun execute(command: UpdateProductCommand): ProductItemResult {
        validateCommand(command)
        val existingProduct = productReadRepository.findById(command.productId)
            .orElseThrow { BusinessException("PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다.") }
        val targetName = command.name ?: existingProduct.name
        val targetDescription = command.description ?: existingProduct.description
        val targetPricePoints = command.pricePoints ?: existingProduct.pricePoints
        val targetStock = command.stock ?: existingProduct.stock
        val targetStatus = command.status ?: existingProduct.status

        val updatedRows = productWriteRepository.updateProduct(
            productId = command.productId,
            name = targetName,
            description = targetDescription,
            pricePoints = targetPricePoints,
            stock = targetStock,
            status = targetStatus,
        )
        if (updatedRows == 0) throw BusinessException("PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다.")
        val updatedProduct = productReadRepository.findById(command.productId)
            .orElseThrow { BusinessException("PRODUCT_NOT_FOUND", "수정된 상품을 찾을 수 없습니다.") }
        return ProductItemResult(
            productId = requireNotNull(updatedProduct.id),
            name = updatedProduct.name,
            description = updatedProduct.description,
            pricePoints = updatedProduct.pricePoints,
            stock = updatedProduct.stock,
            status = updatedProduct.status,
            createdAt = updatedProduct.createdAt,
            updatedAt = updatedProduct.updatedAt,
        )
    }

    private fun validateCommand(command: UpdateProductCommand) {
        if (command.productId <= 0) {
            throw BusinessException("PRODUCT_INVALID_REQUEST", "상품 식별자는 1 이상이어야 합니다.")
        }
        if (command.name == null && command.description == null &&
            command.pricePoints == null && command.stock == null && command.status == null
        ) {
            throw BusinessException("PRODUCT_INVALID_REQUEST", "수정할 필드가 없습니다.")
        }
    }
}

data class UpdateProductCommand(
    val productId: Long,
    val name: String? = null,
    val description: String? = null,
    val pricePoints: Int? = null,
    val stock: Int? = null,
    val status: ProductStatus? = null,
)
