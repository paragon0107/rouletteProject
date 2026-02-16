package com.roulette.backend.domain.budget.service

import com.roulette.backend.common.exception.BusinessException
import com.roulette.backend.common.util.BusinessConstants
import com.roulette.backend.domain.budget.repository.DailyBudgetReadRepository
import com.roulette.backend.domain.budget.repository.DailyBudgetWriteRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneOffset

@Service
class GetTodayBudgetUseCase(
    private val dailyBudgetReadRepository: DailyBudgetReadRepository,
    private val dailyBudgetWriteRepository: DailyBudgetWriteRepository,
) {
    @Transactional
    fun execute(
        date: LocalDate = LocalDate.now(ZoneOffset.UTC),
    ): DailyBudgetQueryResult {
        ensureDailyBudget(date)
        val dailyBudget = dailyBudgetReadRepository.findByBudgetDate(date)
            ?: throw BusinessException("BUDGET_NOT_FOUND", "일일 예산 정보를 찾을 수 없습니다.")
        return DailyBudgetQueryResult(
            date = dailyBudget.budgetDate,
            totalBudgetPoints = dailyBudget.totalBudgetPoints,
            usedBudgetPoints = dailyBudget.usedBudgetPoints,
            remainingBudgetPoints = dailyBudget.remainingBudgetPoints(),
        )
    }

    private fun ensureDailyBudget(targetDate: LocalDate) {
        val existingBudget = dailyBudgetReadRepository.findByBudgetDate(targetDate)
        if (existingBudget != null) return
        try {
            dailyBudgetWriteRepository.insertDailyBudget(
                budgetDate = targetDate,
                totalBudgetPoints = BusinessConstants.DEFAULT_DAILY_BUDGET_POINTS,
            )
        } catch (_: DataIntegrityViolationException) {
            // 동시 생성 경합으로 이미 생성된 경우는 무시한다.
        }
    }
}

data class DailyBudgetQueryResult(
    val date: LocalDate,
    val totalBudgetPoints: Int,
    val usedBudgetPoints: Int,
    val remainingBudgetPoints: Int,
)
