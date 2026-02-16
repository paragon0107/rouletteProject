package com.roulette.backend.domain.product.service

import com.roulette.backend.common.exception.BusinessException
import com.roulette.backend.domain.product.domain.ProductStatus
import com.roulette.backend.domain.product.repository.ProductReadRepository
import com.roulette.backend.domain.product.repository.ProductWriteRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateProductUseCase(
    private val productWriteRepository: ProductWriteRepository,
    private val productReadRepository: ProductReadRepository,
) {
    @Transactional
    fun execute(command: CreateProductCommand): ProductItemResult {
        validateCommand(command)
        val productId = try {
            productWriteRepository.insertProduct(
                name = command.name,
                description = command.description,
                pricePoints = command.pricePoints,
                stock = command.stock,
                status = command.status,
            )
        } catch (_: DataIntegrityViolationException) {
            throw BusinessException("PRODUCT_CONFLICT", "이미 존재하는 상품명입니다.")
        }
        val createdProduct = productReadRepository.findById(productId)
            .orElseThrow { BusinessException("PRODUCT_NOT_FOUND", "생성된 상품을 찾을 수 없습니다.") }
        return ProductItemResult(
            productId = requireNotNull(createdProduct.id),
            name = createdProduct.name,
            description = createdProduct.description,
            pricePoints = createdProduct.pricePoints,
            stock = createdProduct.stock,
            status = createdProduct.status,
            createdAt = createdProduct.createdAt,
            updatedAt = createdProduct.updatedAt,
        )
    }

    private fun validateCommand(command: CreateProductCommand) {
        if (command.name.isBlank() || command.description.isBlank()) {
            throw BusinessException("PRODUCT_INVALID_REQUEST", "상품명/설명은 비어 있을 수 없습니다.")
        }
        if (command.pricePoints < 1 || command.stock < 0) {
            throw BusinessException("PRODUCT_INVALID_REQUEST", "가격/재고 값이 유효하지 않습니다.")
        }
    }
}

data class CreateProductCommand(
    val name: String,
    val description: String,
    val pricePoints: Int,
    val stock: Int,
    val status: ProductStatus,
)
