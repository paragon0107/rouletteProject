package com.roulette.backend.domain.roulette.controller

import com.roulette.backend.common.response.ApiResponse
import com.roulette.backend.common.util.RequestHeaderContextResolver
import com.roulette.backend.domain.roulette.dto.RouletteParticipationResponse
import com.roulette.backend.domain.roulette.dto.TodayParticipationStatusResponse
import com.roulette.backend.domain.roulette.dto.toResponse
import com.roulette.backend.domain.roulette.service.GetTodayParticipationStatusUseCase
import com.roulette.backend.domain.roulette.service.ParticipateRouletteCommand
import com.roulette.backend.domain.roulette.service.ParticipateRouletteUseCase
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/roulette/participations")
class RouletteController(
    private val participateRouletteUseCase: ParticipateRouletteUseCase,
    private val getTodayParticipationStatusUseCase: GetTodayParticipationStatusUseCase,
    private val requestHeaderContextResolver: RequestHeaderContextResolver,
) {
    @PostMapping
    fun participate(request: HttpServletRequest): ApiResponse<RouletteParticipationResponse> {
        val userId = requestHeaderContextResolver.resolveUserId(request)
        val result = participateRouletteUseCase.execute(ParticipateRouletteCommand(userId = userId))
        return ApiResponse(result.toResponse())
    }

    @GetMapping("/today")
    fun getTodayStatus(request: HttpServletRequest): ApiResponse<TodayParticipationStatusResponse> {
        val userId = requestHeaderContextResolver.resolveUserId(request)
        val result = getTodayParticipationStatusUseCase.execute(userId)
        return ApiResponse(result.toResponse())
    }
}
