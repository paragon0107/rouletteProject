package com.roulette.backend.domain.budget.repository

import com.roulette.backend.common.exception.BusinessException
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

@Repository
class DailyBudgetWriteRepository(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {
    fun insertDailyBudget(
        budgetDate: LocalDate,
        totalBudgetPoints: Int,
    ): Long {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val parameters = MapSqlParameterSource()
            .addValue("budgetDate", budgetDate)
            .addValue("totalBudgetPoints", totalBudgetPoints)
            .addValue("usedBudgetPoints", 0)
            .addValue("version", 0L)
            .addValue("createdAt", now)
            .addValue("updatedAt", now)
        val keyHolder = GeneratedKeyHolder()
        namedParameterJdbcTemplate.update(SQL_INSERT_DAILY_BUDGET, parameters, keyHolder, arrayOf("id"))
        return keyHolder.key?.toLong()
            ?: throw BusinessException("BUDGET_INSERT_FAILED", "일일 예산 생성에 실패했습니다.")
    }

    fun allocateBudgetIfPossible(
        budgetDate: LocalDate,
        points: Int,
    ): Int {
        val parameters = MapSqlParameterSource()
            .addValue("budgetDate", budgetDate)
            .addValue("points", points)
            .addValue("updatedAt", LocalDateTime.now(ZoneOffset.UTC))
        return namedParameterJdbcTemplate.update(SQL_ALLOCATE_BUDGET_IF_POSSIBLE, parameters)
    }

    fun releaseBudget(
        budgetDate: LocalDate,
        points: Int,
    ): Int {
        val parameters = MapSqlParameterSource()
            .addValue("budgetDate", budgetDate)
            .addValue("points", points)
            .addValue("updatedAt", LocalDateTime.now(ZoneOffset.UTC))
        return namedParameterJdbcTemplate.update(SQL_RELEASE_BUDGET, parameters)
    }

    fun updateTotalBudgetIfPossible(
        budgetDate: LocalDate,
        totalBudgetPoints: Int,
    ): Int {
        val parameters = MapSqlParameterSource()
            .addValue("budgetDate", budgetDate)
            .addValue("totalBudgetPoints", totalBudgetPoints)
            .addValue("updatedAt", LocalDateTime.now(ZoneOffset.UTC))
        return namedParameterJdbcTemplate.update(SQL_UPDATE_TOTAL_BUDGET_IF_POSSIBLE, parameters)
    }

    companion object {
        private const val SQL_INSERT_DAILY_BUDGET = """
            INSERT INTO daily_budgets (
                budget_date,
                total_budget_points,
                used_budget_points,
                version,
                created_at,
                updated_at
            ) VALUES (
                :budgetDate,
                :totalBudgetPoints,
                :usedBudgetPoints,
                :version,
                :createdAt,
                :updatedAt
            )
        """

        private const val SQL_ALLOCATE_BUDGET_IF_POSSIBLE = """
            UPDATE daily_budgets
            SET used_budget_points = used_budget_points + :points,
                version = version + 1,
                updated_at = :updatedAt
            WHERE budget_date = :budgetDate
              AND used_budget_points + :points <= total_budget_points
        """

        private const val SQL_RELEASE_BUDGET = """
            UPDATE daily_budgets
            SET used_budget_points = used_budget_points - :points,
                version = version + 1,
                updated_at = :updatedAt
            WHERE budget_date = :budgetDate
              AND used_budget_points >= :points
        """

        private const val SQL_UPDATE_TOTAL_BUDGET_IF_POSSIBLE = """
            UPDATE daily_budgets
            SET total_budget_points = :totalBudgetPoints,
                version = version + 1,
                updated_at = :updatedAt
            WHERE budget_date = :budgetDate
              AND used_budget_points <= :totalBudgetPoints
        """
    }
}
