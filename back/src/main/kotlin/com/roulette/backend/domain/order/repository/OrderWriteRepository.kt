package com.roulette.backend.domain.order.repository

import com.roulette.backend.common.exception.BusinessException
import com.roulette.backend.domain.order.domain.OrderStatus
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneOffset

@Repository
class OrderWriteRepository(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {
    fun insertOrder(
        userId: Long,
        productId: Long,
        quantity: Int,
        usedPoints: Int,
        orderedAt: LocalDateTime,
    ): Long {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val parameters = MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("productId", productId)
            .addValue("quantity", quantity)
            .addValue("usedPoints", usedPoints)
            .addValue("status", OrderStatus.PLACED.name)
            .addValue("orderedAt", orderedAt)
            .addValue("version", 0L)
            .addValue("createdAt", now)
            .addValue("updatedAt", now)
        val keyHolder = GeneratedKeyHolder()
        namedParameterJdbcTemplate.update(SQL_INSERT_ORDER, parameters, keyHolder, arrayOf("id"))
        return keyHolder.key?.toLong()
            ?: throw BusinessException("ORDER_INSERT_FAILED", "주문 생성에 실패했습니다.")
    }

    fun cancelOrder(orderId: Long): Int {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val parameters = MapSqlParameterSource()
            .addValue("id", orderId)
            .addValue("placedStatus", OrderStatus.PLACED.name)
            .addValue("canceledStatus", OrderStatus.CANCELED.name)
            .addValue("canceledAt", now)
            .addValue("updatedAt", now)
        return namedParameterJdbcTemplate.update(SQL_CANCEL_ORDER, parameters)
    }

    fun updateStatusIfPossible(
        orderId: Long,
        newStatus: OrderStatus,
    ): Int {
        val parameters = MapSqlParameterSource()
            .addValue("id", orderId)
            .addValue("newStatus", newStatus.name)
            .addValue("canceledStatus", OrderStatus.CANCELED.name)
            .addValue("updatedAt", LocalDateTime.now(ZoneOffset.UTC))
        return namedParameterJdbcTemplate.update(SQL_UPDATE_STATUS_IF_POSSIBLE, parameters)
    }

    companion object {
        private const val SQL_INSERT_ORDER = """
            INSERT INTO orders (
                user_id,
                product_id,
                quantity,
                used_points,
                status,
                ordered_at,
                canceled_at,
                version,
                created_at,
                updated_at
            ) VALUES (
                :userId,
                :productId,
                :quantity,
                :usedPoints,
                :status,
                :orderedAt,
                NULL,
                :version,
                :createdAt,
                :updatedAt
            )
        """

        private const val SQL_CANCEL_ORDER = """
            UPDATE orders
            SET status = :canceledStatus,
                canceled_at = :canceledAt,
                version = version + 1,
                updated_at = :updatedAt
            WHERE id = :id
              AND status = :placedStatus
        """

        private const val SQL_UPDATE_STATUS_IF_POSSIBLE = """
            UPDATE orders
            SET status = :newStatus,
                version = version + 1,
                updated_at = :updatedAt
            WHERE id = :id
              AND status <> :canceledStatus
              AND status <> :newStatus
        """
    }
}
