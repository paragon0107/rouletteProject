package com.roulette.backend.domain.point.controller

import com.roulette.backend.common.response.ApiResponse
import com.roulette.backend.common.util.RequestHeaderContextResolver
import com.roulette.backend.domain.point.dto.ExpiringPointsResponse
import com.roulette.backend.domain.point.dto.PointBalanceResponse
import com.roulette.backend.domain.point.dto.PointUnitListResponse
import com.roulette.backend.domain.point.dto.toResponse
import com.roulette.backend.domain.point.service.GetExpiringPointsUseCase
import com.roulette.backend.domain.point.service.GetMyPointUnitsUseCase
import com.roulette.backend.domain.point.service.GetPointBalanceUseCase
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/points")
class PointController(
    private val getMyPointUnitsUseCase: GetMyPointUnitsUseCase,
    private val getPointBalanceUseCase: GetPointBalanceUseCase,
    private val getExpiringPointsUseCase: GetExpiringPointsUseCase,
    private val requestHeaderContextResolver: RequestHeaderContextResolver,
) {
    @GetMapping("/me")
    fun getMyPointUnits(request: HttpServletRequest): ApiResponse<PointUnitListResponse> {
        val userId = requestHeaderContextResolver.resolveUserId(request)
        val result = getMyPointUnitsUseCase.execute(userId)
        return ApiResponse(result.toResponse())
    }

    @GetMapping("/me/balance")
    fun getMyPointBalance(request: HttpServletRequest): ApiResponse<PointBalanceResponse> {
        val userId = requestHeaderContextResolver.resolveUserId(request)
        val result = getPointBalanceUseCase.execute(userId)
        return ApiResponse(result.toResponse())
    }

    @GetMapping("/me/expiring")
    fun getMyExpiringPoints(
        request: HttpServletRequest,
        @RequestParam(required = false) withinDays: Long?,
    ): ApiResponse<ExpiringPointsResponse> {
        val userId = requestHeaderContextResolver.resolveUserId(request)
        val targetWithinDays = withinDays ?: 7L
        val result = getExpiringPointsUseCase.execute(userId, targetWithinDays)
        return ApiResponse(result.toResponse())
    }
}
