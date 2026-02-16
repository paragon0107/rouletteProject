package com.roulette.backend.domain.roulette.repository

import com.roulette.backend.common.exception.BusinessException
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

@Repository
class RouletteParticipationWriteRepository(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {
    fun insertParticipation(
        userId: Long,
        participationDate: LocalDate,
        awardedPoints: Int,
        awardedAt: LocalDateTime,
        pointExpiresAt: LocalDateTime,
    ): Long {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val parameters = MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("participationDate", participationDate)
            .addValue("awardedPoints", awardedPoints)
            .addValue("awardedAt", awardedAt)
            .addValue("pointExpiresAt", pointExpiresAt)
            .addValue("isCanceled", false)
            .addValue("version", 0L)
            .addValue("createdAt", now)
            .addValue("updatedAt", now)
        val keyHolder = GeneratedKeyHolder()
        namedParameterJdbcTemplate.update(SQL_INSERT_PARTICIPATION, parameters, keyHolder, arrayOf("id"))
        return keyHolder.key?.toLong()
            ?: throw BusinessException("ROULETTE_INSERT_FAILED", "룰렛 참여 저장에 실패했습니다.")
    }

    fun cancelParticipation(participationId: Long): Int {
        val parameters = MapSqlParameterSource()
            .addValue("id", participationId)
            .addValue("canceledAt", LocalDateTime.now(ZoneOffset.UTC))
            .addValue("updatedAt", LocalDateTime.now(ZoneOffset.UTC))
        return namedParameterJdbcTemplate.update(SQL_CANCEL_PARTICIPATION, parameters)
    }

    companion object {
        private const val SQL_INSERT_PARTICIPATION = """
            INSERT INTO roulette_participations (
                user_id,
                participation_date,
                awarded_points,
                awarded_at,
                point_expires_at,
                is_canceled,
                canceled_at,
                version,
                created_at,
                updated_at
            ) VALUES (
                :userId,
                :participationDate,
                :awardedPoints,
                :awardedAt,
                :pointExpiresAt,
                :isCanceled,
                NULL,
                :version,
                :createdAt,
                :updatedAt
            )
        """

        private const val SQL_CANCEL_PARTICIPATION = """
            UPDATE roulette_participations
            SET is_canceled = TRUE,
                canceled_at = :canceledAt,
                version = version + 1,
                updated_at = :updatedAt
            WHERE id = :id
              AND is_canceled = FALSE
        """
    }
}
