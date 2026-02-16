package com.roulette.backend.domain.product.repository

import com.roulette.backend.domain.product.domain.Product
import com.roulette.backend.domain.product.domain.ProductStatus
import org.springframework.data.jpa.repository.JpaRepository

interface ProductReadRepository : JpaRepository<Product, Long> {
    fun findByName(name: String): Product?

    fun findAllByStatus(status: ProductStatus): List<Product>

    fun findAllByOrderByCreatedAtDesc(): List<Product>
}
