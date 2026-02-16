package com.roulette.backend.domain.roulette.service

import com.roulette.backend.common.exception.BusinessException
import com.roulette.backend.domain.budget.repository.DailyBudgetWriteRepository
import com.roulette.backend.domain.point.domain.PointEventType
import com.roulette.backend.domain.point.domain.PointTransactionDirection
import com.roulette.backend.domain.point.domain.PointUnitStatus
import com.roulette.backend.domain.point.repository.PointTransactionWriteRepository
import com.roulette.backend.domain.point.repository.PointUnitReadRepository
import com.roulette.backend.domain.point.repository.PointUnitWriteRepository
import com.roulette.backend.domain.roulette.repository.RouletteParticipationReadRepository
import com.roulette.backend.domain.roulette.repository.RouletteParticipationWriteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class CancelRouletteParticipationUseCase(
    private val rouletteParticipationReadRepository: RouletteParticipationReadRepository,
    private val rouletteParticipationWriteRepository: RouletteParticipationWriteRepository,
    private val dailyBudgetWriteRepository: DailyBudgetWriteRepository,
    private val pointUnitReadRepository: PointUnitReadRepository,
    private val pointUnitWriteRepository: PointUnitWriteRepository,
    private val pointTransactionWriteRepository: PointTransactionWriteRepository,
) {
    @Transactional
    fun execute(participationId: Long): CancelRouletteParticipationResult {
        val participation = rouletteParticipationReadRepository.findById(participationId)
            .orElseThrow { BusinessException("ROULETTE_PARTICIPATION_NOT_FOUND", "룰렛 참여를 찾을 수 없습니다.") }
        if (participation.canceled) {
            throw BusinessException("ROULETTE_PARTICIPATION_ALREADY_CANCELED", "이미 취소된 룰렛 참여입니다.")
        }

        rejectIfRewardPointAlreadyUsed(participationId, participation.awardedPoints)
        cancelParticipationOrThrow(participationId)
        releaseBudgetOrThrow(participation.participationDate, participation.awardedPoints)
        pointUnitWriteRepository.cancelBySourceParticipationId(participationId)

        val canceledAt = LocalDateTime.now(ZoneOffset.UTC)
        pointTransactionWriteRepository.insertTransaction(
            userId = participation.userId,
            eventType = PointEventType.ROULETTE_REVOKE,
            direction = PointTransactionDirection.DEBIT,
            amount = participation.awardedPoints,
            occurredAt = canceledAt,
            participationId = participationId,
        )
        return CancelRouletteParticipationResult(
            participationId = participationId,
            userId = participation.userId,
            recoveredPoints = participation.awardedPoints,
            canceledAt = canceledAt,
        )
    }

    private fun rejectIfRewardPointAlreadyUsed(
        participationId: Long,
        awardedPoints: Int,
    ) {
        val pointUnits = pointUnitReadRepository.findAllBySourceParticipationId(participationId)
        val recoverablePoints = pointUnits
            .filter { it.status == PointUnitStatus.AVAILABLE }
            .sumOf { it.remainingAmount }
        if (recoverablePoints >= awardedPoints) return
        throw BusinessException("POINT_ALREADY_USED", "지급 포인트가 이미 사용되어 회수할 수 없습니다.")
    }

    private fun cancelParticipationOrThrow(participationId: Long) {
        val updatedRows = rouletteParticipationWriteRepository.cancelParticipation(participationId)
        if (updatedRows > 0) return
        throw BusinessException("ROULETTE_PARTICIPATION_ALREADY_CANCELED", "이미 취소된 룰렛 참여입니다.")
    }

    private fun releaseBudgetOrThrow(
        participationDate: java.time.LocalDate,
        points: Int,
    ) {
        val updatedRows = dailyBudgetWriteRepository.releaseBudget(participationDate, points)
        if (updatedRows > 0) return
        throw BusinessException("BUDGET_RELEASE_FAILED", "예산 회수 처리에 실패했습니다.")
    }
}

data class CancelRouletteParticipationResult(
    val participationId: Long,
    val userId: Long,
    val recoveredPoints: Int,
    val canceledAt: LocalDateTime,
)
