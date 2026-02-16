package com.roulette.backend.domain.point.repository

import com.roulette.backend.common.exception.BusinessException
import com.roulette.backend.domain.point.domain.PointEventType
import com.roulette.backend.domain.point.domain.PointTransactionDirection
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneOffset

@Repository
class PointTransactionWriteRepository(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {
    fun insertTransaction(
        userId: Long,
        eventType: PointEventType,
        direction: PointTransactionDirection,
        amount: Int,
        occurredAt: LocalDateTime,
        orderId: Long? = null,
        participationId: Long? = null,
    ): Long {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val parameters = MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("eventType", eventType.name)
            .addValue("direction", direction.name)
            .addValue("amount", amount)
            .addValue("occurredAt", occurredAt)
            .addValue("orderId", orderId)
            .addValue("participationId", participationId)
            .addValue("createdAt", now)
            .addValue("updatedAt", now)
        val keyHolder = GeneratedKeyHolder()
        namedParameterJdbcTemplate.update(SQL_INSERT_POINT_TRANSACTION, parameters, keyHolder, arrayOf("id"))
        return keyHolder.key?.toLong()
            ?: throw BusinessException("POINT_TRANSACTION_INSERT_FAILED", "포인트 이력 생성에 실패했습니다.")
    }

    companion object {
        private const val SQL_INSERT_POINT_TRANSACTION = """
            INSERT INTO point_transactions (
                user_id,
                event_type,
                direction,
                amount,
                occurred_at,
                order_id,
                participation_id,
                created_at,
                updated_at
            ) VALUES (
                :userId,
                :eventType,
                :direction,
                :amount,
                :occurredAt,
                :orderId,
                :participationId,
                :createdAt,
                :updatedAt
            )
        """
    }
}
