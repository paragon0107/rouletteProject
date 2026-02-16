package com.roulette.backend.domain.budget.controller

import com.roulette.backend.common.response.ApiResponse
import com.roulette.backend.common.util.RequestHeaderContextResolver
import com.roulette.backend.domain.budget.dto.BudgetResponse
import com.roulette.backend.domain.budget.dto.toResponse
import com.roulette.backend.domain.budget.service.GetTodayBudgetUseCase
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/budgets")
class BudgetController(
    private val getTodayBudgetUseCase: GetTodayBudgetUseCase,
    private val requestHeaderContextResolver: RequestHeaderContextResolver,
) {
    @GetMapping("/today")
    fun getTodayBudget(request: HttpServletRequest): ApiResponse<BudgetResponse> {
        requestHeaderContextResolver.resolveUserId(request)
        val result = getTodayBudgetUseCase.execute()
        return ApiResponse(result.toResponse())
    }
}
