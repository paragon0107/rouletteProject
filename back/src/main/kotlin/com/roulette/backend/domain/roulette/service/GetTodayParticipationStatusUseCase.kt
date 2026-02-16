package com.roulette.backend.domain.roulette.service

import com.roulette.backend.domain.roulette.repository.RouletteParticipationReadRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneOffset

@Service
class GetTodayParticipationStatusUseCase(
    private val rouletteParticipationReadRepository: RouletteParticipationReadRepository,
) {
    @Transactional(readOnly = true)
    fun execute(
        userId: Long,
        participationDate: LocalDate = LocalDate.now(ZoneOffset.UTC),
    ): TodayParticipationStatusResult {
        val participation = rouletteParticipationReadRepository
            .findByUserIdAndParticipationDateAndCanceledFalse(userId, participationDate)
        if (participation == null) {
            return TodayParticipationStatusResult(
                participationDate = participationDate,
                participatedToday = false,
                participationId = null,
                awardedPoints = null,
                participatedAt = null,
            )
        }
        return TodayParticipationStatusResult(
            participationDate = participationDate,
            participatedToday = true,
            participationId = requireNotNull(participation.id),
            awardedPoints = participation.awardedPoints,
            participatedAt = participation.awardedAt,
        )
    }
}

data class TodayParticipationStatusResult(
    val participationDate: LocalDate,
    val participatedToday: Boolean,
    val participationId: Long?,
    val awardedPoints: Int?,
    val participatedAt: java.time.LocalDateTime?,
)
