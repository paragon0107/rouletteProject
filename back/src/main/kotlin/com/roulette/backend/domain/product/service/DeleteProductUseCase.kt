package com.roulette.backend.domain.product.service

import com.roulette.backend.common.exception.BusinessException
import com.roulette.backend.domain.product.repository.ProductWriteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeleteProductUseCase(
    private val productWriteRepository: ProductWriteRepository,
) {
    @Transactional
    fun execute(productId: Long) {
        if (productId <= 0) {
            throw BusinessException("PRODUCT_INVALID_REQUEST", "상품 식별자는 1 이상이어야 합니다.")
        }
        val deletedRows = productWriteRepository.deleteProduct(productId)
        if (deletedRows > 0) return
        throw BusinessException("PRODUCT_NOT_FOUND", "삭제할 상품을 찾을 수 없습니다.")
    }
}
