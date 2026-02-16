package com.roulette.backend.domain.budget.dto

import com.roulette.backend.domain.budget.service.DailyBudgetQueryResult
import java.time.LocalDate

data class BudgetResponse(
    val date: LocalDate,
    val totalBudget: Int,
    val usedBudget: Int,
    val remainingBudget: Int,
)

fun DailyBudgetQueryResult.toResponse(): BudgetResponse {
    return BudgetResponse(
        date = date,
        totalBudget = totalBudgetPoints,
        usedBudget = usedBudgetPoints,
        remainingBudget = remainingBudgetPoints,
    )
}
