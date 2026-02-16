package com.roulette.backend.domain.roulette.controller

import com.roulette.backend.common.response.ApiResponse
import com.roulette.backend.common.util.RequestHeaderContextResolver
import com.roulette.backend.domain.roulette.dto.CancelRouletteParticipationResponse
import com.roulette.backend.domain.roulette.dto.RouletteParticipantCountResponse
import com.roulette.backend.domain.roulette.dto.RouletteParticipantListResponse
import com.roulette.backend.domain.roulette.dto.toResponse
import com.roulette.backend.domain.roulette.service.CancelRouletteParticipationUseCase
import com.roulette.backend.domain.roulette.service.GetRouletteParticipantsUseCase
import jakarta.servlet.http.HttpServletRequest
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.ZoneOffset

@RestController
@RequestMapping("/api/v1/admin/roulette")
class AdminRouletteController(
    private val getRouletteParticipantsUseCase: GetRouletteParticipantsUseCase,
    private val cancelRouletteParticipationUseCase: CancelRouletteParticipationUseCase,
    private val requestHeaderContextResolver: RequestHeaderContextResolver,
) {
    @GetMapping("/participants/count")
    fun getParticipantCount(
        request: HttpServletRequest,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?,
    ): ApiResponse<RouletteParticipantCountResponse> {
        requestHeaderContextResolver.requireAdmin(request)
        val targetDate = date ?: LocalDate.now(ZoneOffset.UTC)
        val result = getRouletteParticipantsUseCase.countByDate(targetDate)
        return ApiResponse(result.toResponse())
    }

    @GetMapping("/participants")
    fun getParticipantList(
        request: HttpServletRequest,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?,
    ): ApiResponse<RouletteParticipantListResponse> {
        requestHeaderContextResolver.requireAdmin(request)
        val targetDate = date ?: LocalDate.now(ZoneOffset.UTC)
        val result = getRouletteParticipantsUseCase.listByDate(targetDate)
        return ApiResponse(result.toResponse())
    }

    @PostMapping("/participations/{participationId}/cancel")
    fun cancelParticipation(
        request: HttpServletRequest,
        @PathVariable participationId: Long,
    ): ApiResponse<CancelRouletteParticipationResponse> {
        requestHeaderContextResolver.requireAdmin(request)
        val result = cancelRouletteParticipationUseCase.execute(participationId)
        return ApiResponse(result.toResponse())
    }
}
