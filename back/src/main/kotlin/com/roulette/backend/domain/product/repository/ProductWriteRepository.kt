package com.roulette.backend.domain.product.repository

import com.roulette.backend.common.exception.BusinessException
import com.roulette.backend.domain.product.domain.ProductStatus
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneOffset

@Repository
class ProductWriteRepository(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {
    fun insertProduct(
        name: String,
        description: String,
        pricePoints: Int,
        stock: Int,
        status: ProductStatus,
    ): Long {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val parameters = MapSqlParameterSource()
            .addValue("name", name.trim())
            .addValue("description", description.trim())
            .addValue("pricePoints", pricePoints)
            .addValue("stock", stock)
            .addValue("status", status.name)
            .addValue("version", 0L)
            .addValue("createdAt", now)
            .addValue("updatedAt", now)
        val keyHolder = GeneratedKeyHolder()
        namedParameterJdbcTemplate.update(SQL_INSERT_PRODUCT, parameters, keyHolder, arrayOf("id"))
        return keyHolder.key?.toLong()
            ?: throw BusinessException("PRODUCT_INSERT_FAILED", "상품 생성에 실패했습니다.")
    }

    fun updateProduct(
        productId: Long,
        name: String,
        description: String,
        pricePoints: Int,
        stock: Int,
        status: ProductStatus,
    ): Int {
        val parameters = MapSqlParameterSource()
            .addValue("id", productId)
            .addValue("name", name.trim())
            .addValue("description", description.trim())
            .addValue("pricePoints", pricePoints)
            .addValue("stock", stock)
            .addValue("status", status.name)
            .addValue("updatedAt", LocalDateTime.now(ZoneOffset.UTC))
        return namedParameterJdbcTemplate.update(SQL_UPDATE_PRODUCT, parameters)
    }

    fun deleteProduct(productId: Long): Int {
        return namedParameterJdbcTemplate.update(
            SQL_DELETE_PRODUCT,
            MapSqlParameterSource().addValue("id", productId),
        )
    }

    fun decreaseStockIfPossible(
        productId: Long,
        quantity: Int,
    ): Int {
        val parameters = MapSqlParameterSource()
            .addValue("id", productId)
            .addValue("quantity", quantity)
            .addValue("activeStatus", ProductStatus.ACTIVE.name)
            .addValue("updatedAt", LocalDateTime.now(ZoneOffset.UTC))
        return namedParameterJdbcTemplate.update(SQL_DECREASE_STOCK_IF_POSSIBLE, parameters)
    }

    fun increaseStock(
        productId: Long,
        quantity: Int,
    ): Int {
        val parameters = MapSqlParameterSource()
            .addValue("id", productId)
            .addValue("quantity", quantity)
            .addValue("updatedAt", LocalDateTime.now(ZoneOffset.UTC))
        return namedParameterJdbcTemplate.update(SQL_INCREASE_STOCK, parameters)
    }

    companion object {
        private const val SQL_INSERT_PRODUCT = """
            INSERT INTO products (
                name,
                description,
                price_points,
                stock,
                status,
                version,
                created_at,
                updated_at
            ) VALUES (
                :name,
                :description,
                :pricePoints,
                :stock,
                :status,
                :version,
                :createdAt,
                :updatedAt
            )
        """

        private const val SQL_UPDATE_PRODUCT = """
            UPDATE products
            SET name = :name,
                description = :description,
                price_points = :pricePoints,
                stock = :stock,
                status = :status,
                version = version + 1,
                updated_at = :updatedAt
            WHERE id = :id
        """

        private const val SQL_DELETE_PRODUCT = """
            DELETE FROM products
            WHERE id = :id
        """

        private const val SQL_DECREASE_STOCK_IF_POSSIBLE = """
            UPDATE products
            SET stock = stock - :quantity,
                version = version + 1,
                updated_at = :updatedAt
            WHERE id = :id
              AND status = :activeStatus
              AND stock >= :quantity
        """

        private const val SQL_INCREASE_STOCK = """
            UPDATE products
            SET stock = stock + :quantity,
                version = version + 1,
                updated_at = :updatedAt
            WHERE id = :id
        """
    }
}
