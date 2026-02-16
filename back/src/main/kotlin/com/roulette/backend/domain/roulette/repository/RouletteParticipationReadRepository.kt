package com.roulette.backend.domain.roulette.repository

import com.roulette.backend.domain.roulette.domain.RouletteParticipation
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface RouletteParticipationReadRepository : JpaRepository<RouletteParticipation, Long> {
    fun existsByUserIdAndParticipationDateAndCanceledFalse(
        userId: Long,
        participationDate: LocalDate,
    ): Boolean

    fun findByUserIdAndParticipationDateAndCanceledFalse(
        userId: Long,
        participationDate: LocalDate,
    ): RouletteParticipation?

    fun countByParticipationDateAndCanceledFalse(participationDate: LocalDate): Long

    fun findAllByParticipationDateOrderByAwardedAtDesc(participationDate: LocalDate): List<RouletteParticipation>
}
