package com.roulette.backend.domain.budget.service

import com.roulette.backend.common.exception.BusinessException
import com.roulette.backend.domain.budget.repository.DailyBudgetReadRepository
import com.roulette.backend.domain.budget.repository.DailyBudgetWriteRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class UpdateDailyBudgetUseCase(
    private val dailyBudgetReadRepository: DailyBudgetReadRepository,
    private val dailyBudgetWriteRepository: DailyBudgetWriteRepository,
) {
    @Transactional
    fun execute(
        budgetDate: LocalDate,
        totalBudgetPoints: Int,
    ): DailyBudgetQueryResult {
        if (totalBudgetPoints < 0) {
            throw BusinessException("BUDGET_INVALID_TOTAL", "총 예산은 0 이상이어야 합니다.")
        }
        ensureBudgetRowExists(budgetDate, totalBudgetPoints)
        val updatedRows = dailyBudgetWriteRepository.updateTotalBudgetIfPossible(
            budgetDate = budgetDate,
            totalBudgetPoints = totalBudgetPoints,
        )
        if (updatedRows == 0) {
            throw BusinessException(
                "BUDGET_TOTAL_LESS_THAN_USED",
                "총 예산은 현재 사용 예산보다 작을 수 없습니다.",
            )
        }
        val dailyBudget = dailyBudgetReadRepository.findByBudgetDate(budgetDate)
            ?: throw BusinessException("BUDGET_NOT_FOUND", "일일 예산 정보를 찾을 수 없습니다.")
        return DailyBudgetQueryResult(
            date = dailyBudget.budgetDate,
            totalBudgetPoints = dailyBudget.totalBudgetPoints,
            usedBudgetPoints = dailyBudget.usedBudgetPoints,
            remainingBudgetPoints = dailyBudget.remainingBudgetPoints(),
        )
    }

    private fun ensureBudgetRowExists(
        budgetDate: LocalDate,
        totalBudgetPoints: Int,
    ) {
        val existingBudget = dailyBudgetReadRepository.findByBudgetDate(budgetDate)
        if (existingBudget != null) return
        try {
            dailyBudgetWriteRepository.insertDailyBudget(
                budgetDate = budgetDate,
                totalBudgetPoints = totalBudgetPoints,
            )
        } catch (_: DataIntegrityViolationException) {
            // 동시 생성 경합 시 이미 생성된 행을 그대로 사용한다.
        }
    }
}
