package com.roulette.backend.domain.roulette.service

import com.roulette.backend.domain.roulette.repository.RouletteParticipationReadRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneOffset

@Service
class GetRouletteParticipantsUseCase(
    private val rouletteParticipationReadRepository: RouletteParticipationReadRepository,
) {
    @Transactional(readOnly = true)
    fun countByDate(
        participationDate: LocalDate = LocalDate.now(ZoneOffset.UTC),
    ): RouletteParticipantCountResult {
        val participantCount = rouletteParticipationReadRepository
            .countByParticipationDateAndCanceledFalse(participationDate)
        val participantList = rouletteParticipationReadRepository
            .findAllByParticipationDateOrderByAwardedAtDesc(participationDate)
            .filter { !it.canceled }
        val totalAwardedPoints = participantList.sumOf { it.awardedPoints }
        return RouletteParticipantCountResult(
            participationDate = participationDate,
            participantCount = participantCount,
            totalAwardedPoints = totalAwardedPoints,
        )
    }

    @Transactional(readOnly = true)
    fun listByDate(
        participationDate: LocalDate = LocalDate.now(ZoneOffset.UTC),
    ): RouletteParticipantListResult {
        val participantItems = rouletteParticipationReadRepository
            .findAllByParticipationDateOrderByAwardedAtDesc(participationDate)
            .map { participation ->
                RouletteParticipantItemResult(
                    participationId = requireNotNull(participation.id),
                    userId = participation.userId,
                    awardedPoints = participation.awardedPoints,
                    awardedAt = participation.awardedAt,
                    canceled = participation.canceled,
                )
            }
        return RouletteParticipantListResult(
            participationDate = participationDate,
            totalItems = participantItems.size,
            items = participantItems,
        )
    }
}

data class RouletteParticipantCountResult(
    val participationDate: LocalDate,
    val participantCount: Long,
    val totalAwardedPoints: Int,
)

data class RouletteParticipantListResult(
    val participationDate: LocalDate,
    val totalItems: Int,
    val items: List<RouletteParticipantItemResult>,
)

data class RouletteParticipantItemResult(
    val participationId: Long,
    val userId: Long,
    val awardedPoints: Int,
    val awardedAt: java.time.LocalDateTime,
    val canceled: Boolean,
)
