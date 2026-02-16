package com.roulette.backend.domain.point.repository

import com.roulette.backend.common.exception.BusinessException
import com.roulette.backend.domain.point.domain.PointEventType
import com.roulette.backend.domain.point.domain.PointUnitStatus
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneOffset

@Repository
class PointUnitWriteRepository(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {
    fun insertPointUnit(
        userId: Long,
        eventType: PointEventType,
        amount: Int,
        earnedAt: LocalDateTime,
        expiresAt: LocalDateTime,
        sourceParticipationId: Long? = null,
        sourceOrderId: Long? = null,
    ): Long {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val parameters = MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("eventType", eventType.name)
            .addValue("originalAmount", amount)
            .addValue("remainingAmount", amount)
            .addValue("earnedAt", earnedAt)
            .addValue("expiresAt", expiresAt)
            .addValue("status", PointUnitStatus.AVAILABLE.name)
            .addValue("sourceParticipationId", sourceParticipationId)
            .addValue("sourceOrderId", sourceOrderId)
            .addValue("version", 0L)
            .addValue("createdAt", now)
            .addValue("updatedAt", now)
        val keyHolder = GeneratedKeyHolder()
        namedParameterJdbcTemplate.update(SQL_INSERT_POINT_UNIT, parameters, keyHolder, arrayOf("id"))
        return keyHolder.key?.toLong()
            ?: throw BusinessException("POINT_UNIT_INSERT_FAILED", "포인트 단위 생성에 실패했습니다.")
    }

    fun deductAmountIfPossible(
        pointUnitId: Long,
        amount: Int,
    ): Int {
        val parameters = MapSqlParameterSource()
            .addValue("id", pointUnitId)
            .addValue("amount", amount)
            .addValue("updatedAt", LocalDateTime.now(ZoneOffset.UTC))
        return namedParameterJdbcTemplate.update(SQL_DEDUCT_POINT_UNIT_AMOUNT, parameters)
    }

    fun restoreAmountIfPossible(
        pointUnitId: Long,
        amount: Int,
    ): Int {
        val parameters = MapSqlParameterSource()
            .addValue("id", pointUnitId)
            .addValue("amount", amount)
            .addValue("availableStatus", PointUnitStatus.AVAILABLE.name)
            .addValue("usedStatus", PointUnitStatus.USED.name)
            .addValue("updatedAt", LocalDateTime.now(ZoneOffset.UTC))
        return namedParameterJdbcTemplate.update(SQL_RESTORE_POINT_UNIT_AMOUNT, parameters)
    }

    fun cancelPointUnit(pointUnitId: Long): Int {
        val parameters = MapSqlParameterSource()
            .addValue("id", pointUnitId)
            .addValue("canceledStatus", PointUnitStatus.CANCELED.name)
            .addValue("updatedAt", LocalDateTime.now(ZoneOffset.UTC))
        return namedParameterJdbcTemplate.update(SQL_CANCEL_POINT_UNIT, parameters)
    }

    fun cancelBySourceParticipationId(sourceParticipationId: Long): Int {
        val parameters = MapSqlParameterSource()
            .addValue("sourceParticipationId", sourceParticipationId)
            .addValue("canceledStatus", PointUnitStatus.CANCELED.name)
            .addValue("updatedAt", LocalDateTime.now(ZoneOffset.UTC))
        return namedParameterJdbcTemplate.update(SQL_CANCEL_BY_SOURCE_PARTICIPATION_ID, parameters)
    }

    fun expirePointUnits(referenceTime: LocalDateTime): Int {
        val parameters = MapSqlParameterSource()
            .addValue("referenceTime", referenceTime)
            .addValue("availableStatus", PointUnitStatus.AVAILABLE.name)
            .addValue("expiredStatus", PointUnitStatus.EXPIRED.name)
            .addValue("updatedAt", LocalDateTime.now(ZoneOffset.UTC))
        return namedParameterJdbcTemplate.update(SQL_EXPIRE_POINT_UNITS, parameters)
    }

    companion object {
        private const val SQL_INSERT_POINT_UNIT = """
            INSERT INTO point_units (
                user_id,
                event_type,
                original_amount,
                remaining_amount,
                earned_at,
                expires_at,
                status,
                source_participation_id,
                source_order_id,
                version,
                created_at,
                updated_at
            ) VALUES (
                :userId,
                :eventType,
                :originalAmount,
                :remainingAmount,
                :earnedAt,
                :expiresAt,
                :status,
                :sourceParticipationId,
                :sourceOrderId,
                :version,
                :createdAt,
                :updatedAt
            )
        """

        private const val SQL_DEDUCT_POINT_UNIT_AMOUNT = """
            UPDATE point_units
            SET remaining_amount = remaining_amount - :amount,
                status = CASE
                    WHEN remaining_amount - :amount = 0 THEN 'USED'
                    ELSE status
                END,
                version = version + 1,
                updated_at = :updatedAt
            WHERE id = :id
              AND status = 'AVAILABLE'
              AND remaining_amount >= :amount
        """

        private const val SQL_RESTORE_POINT_UNIT_AMOUNT = """
            UPDATE point_units
            SET remaining_amount = remaining_amount + :amount,
                status = :availableStatus,
                version = version + 1,
                updated_at = :updatedAt
            WHERE id = :id
              AND status IN (:availableStatus, :usedStatus)
              AND remaining_amount + :amount <= original_amount
        """

        private const val SQL_CANCEL_POINT_UNIT = """
            UPDATE point_units
            SET remaining_amount = 0,
                status = :canceledStatus,
                version = version + 1,
                updated_at = :updatedAt
            WHERE id = :id
              AND status <> :canceledStatus
        """

        private const val SQL_CANCEL_BY_SOURCE_PARTICIPATION_ID = """
            UPDATE point_units
            SET remaining_amount = 0,
                status = :canceledStatus,
                version = version + 1,
                updated_at = :updatedAt
            WHERE source_participation_id = :sourceParticipationId
              AND status <> :canceledStatus
        """

        private const val SQL_EXPIRE_POINT_UNITS = """
            UPDATE point_units
            SET remaining_amount = 0,
                status = :expiredStatus,
                version = version + 1,
                updated_at = :updatedAt
            WHERE status = :availableStatus
              AND expires_at <= :referenceTime
        """
    }
}
