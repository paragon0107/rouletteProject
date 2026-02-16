package com.roulette.backend.domain.budget.controller

import com.roulette.backend.common.response.ApiResponse
import com.roulette.backend.common.util.RequestHeaderContextResolver
import com.roulette.backend.domain.budget.dto.BudgetResponse
import com.roulette.backend.domain.budget.dto.UpdateDailyBudgetRequest
import com.roulette.backend.domain.budget.dto.toResponse
import com.roulette.backend.domain.budget.service.GetTodayBudgetUseCase
import com.roulette.backend.domain.budget.service.UpdateDailyBudgetUseCase
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/admin/budgets")
class AdminBudgetController(
    private val getTodayBudgetUseCase: GetTodayBudgetUseCase,
    private val updateDailyBudgetUseCase: UpdateDailyBudgetUseCase,
    private val requestHeaderContextResolver: RequestHeaderContextResolver,
) {
    @GetMapping("/{date}")
    fun getDailyBudget(
        request: HttpServletRequest,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
    ): ApiResponse<BudgetResponse> {
        requestHeaderContextResolver.requireAdmin(request)
        val result = getTodayBudgetUseCase.execute(date)
        return ApiResponse(result.toResponse())
    }

    @PutMapping("/{date}")
    fun updateDailyBudget(
        request: HttpServletRequest,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
        @Valid @RequestBody body: UpdateDailyBudgetRequest,
    ): ApiResponse<BudgetResponse> {
        requestHeaderContextResolver.requireAdmin(request)
        val result = updateDailyBudgetUseCase.execute(date, body.totalBudget)
        return ApiResponse(result.toResponse())
    }
}
