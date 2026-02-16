package com.roulette.backend.domain.budget.dto

import jakarta.validation.constraints.Min

data class UpdateDailyBudgetRequest(
    @field:Min(value = 0, message = "총 예산은 0 이상이어야 합니다.")
    val totalBudget: Int,
)
