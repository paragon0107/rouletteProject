package com.roulette.backend.domain.roulette.dto

import com.roulette.backend.domain.roulette.service.CancelRouletteParticipationResult
import com.roulette.backend.domain.roulette.service.ParticipateRouletteResult
import com.roulette.backend.domain.roulette.service.RouletteParticipantCountResult
import com.roulette.backend.domain.roulette.service.RouletteParticipantItemResult
import com.roulette.backend.domain.roulette.service.RouletteParticipantListResult
import com.roulette.backend.domain.roulette.service.TodayParticipationStatusResult
import java.time.LocalDate
import java.time.LocalDateTime

data class RouletteParticipationResponse(
    val participationId: Long,
    val participationDate: LocalDate,
    val awardedPoints: Int,
    val pointExpiresAt: LocalDateTime,
    val remainingBudget: Int,
)

data class TodayParticipationStatusResponse(
    val participationDate: LocalDate,
    val participatedToday: Boolean,
    val participationId: Long?,
    val awardedPoints: Int?,
    val participatedAt: LocalDateTime?,
)

data class RouletteParticipantCountResponse(
    val date: LocalDate,
    val participantCount: Long,
    val totalAwardedPoints: Int,
)

data class RouletteParticipantListResponse(
    val date: LocalDate,
    val totalItems: Int,
    val items: List<RouletteParticipantItemResponse>,
)

data class RouletteParticipantItemResponse(
    val participationId: Long,
    val userId: Long,
    val awardedPoints: Int,
    val awardedAt: LocalDateTime,
    val canceled: Boolean,
)

data class CancelRouletteParticipationResponse(
    val participationId: Long,
    val userId: Long,
    val recoveredPoints: Int,
    val canceledAt: LocalDateTime,
)

fun ParticipateRouletteResult.toResponse(): RouletteParticipationResponse {
    return RouletteParticipationResponse(
        participationId = participationId,
        participationDate = participationDate,
        awardedPoints = awardedPoints,
        pointExpiresAt = pointExpiresAt,
        remainingBudget = remainingBudgetPoints,
    )
}

fun TodayParticipationStatusResult.toResponse(): TodayParticipationStatusResponse {
    return TodayParticipationStatusResponse(
        participationDate = participationDate,
        participatedToday = participatedToday,
        participationId = participationId,
        awardedPoints = awardedPoints,
        participatedAt = participatedAt,
    )
}

fun RouletteParticipantCountResult.toResponse(): RouletteParticipantCountResponse {
    return RouletteParticipantCountResponse(
        date = participationDate,
        participantCount = participantCount,
        totalAwardedPoints = totalAwardedPoints,
    )
}

fun RouletteParticipantListResult.toResponse(): RouletteParticipantListResponse {
    return RouletteParticipantListResponse(
        date = participationDate,
        totalItems = totalItems,
        items = items.map { it.toResponse() },
    )
}

fun RouletteParticipantItemResult.toResponse(): RouletteParticipantItemResponse {
    return RouletteParticipantItemResponse(
        participationId = participationId,
        userId = userId,
        awardedPoints = awardedPoints,
        awardedAt = awardedAt,
        canceled = canceled,
    )
}

fun CancelRouletteParticipationResult.toResponse(): CancelRouletteParticipationResponse {
    return CancelRouletteParticipationResponse(
        participationId = participationId,
        userId = userId,
        recoveredPoints = recoveredPoints,
        canceledAt = canceledAt,
    )
}
