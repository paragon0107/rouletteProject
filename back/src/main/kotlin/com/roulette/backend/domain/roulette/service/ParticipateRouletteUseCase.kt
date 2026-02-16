package com.roulette.backend.domain.roulette.service

import com.roulette.backend.common.exception.BusinessException
import com.roulette.backend.common.util.BusinessConstants
import com.roulette.backend.domain.budget.repository.DailyBudgetReadRepository
import com.roulette.backend.domain.budget.repository.DailyBudgetWriteRepository
import com.roulette.backend.domain.point.domain.PointEventType
import com.roulette.backend.domain.point.domain.PointTransactionDirection
import com.roulette.backend.domain.point.repository.PointTransactionWriteRepository
import com.roulette.backend.domain.point.repository.PointUnitWriteRepository
import com.roulette.backend.domain.roulette.repository.RouletteParticipationReadRepository
import com.roulette.backend.domain.roulette.repository.RouletteParticipationWriteRepository
import com.roulette.backend.domain.user.repository.UserReadRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class ParticipateRouletteUseCase(
    private val userReadRepository: UserReadRepository,
    private val dailyBudgetReadRepository: DailyBudgetReadRepository,
    private val dailyBudgetWriteRepository: DailyBudgetWriteRepository,
    private val rouletteParticipationReadRepository: RouletteParticipationReadRepository,
    private val rouletteParticipationWriteRepository: RouletteParticipationWriteRepository,
    private val pointUnitWriteRepository: PointUnitWriteRepository,
    private val pointTransactionWriteRepository: PointTransactionWriteRepository,
    private val rouletteRewardSelector: RouletteRewardSelector,
) {
    @Transactional
    fun execute(command: ParticipateRouletteCommand): ParticipateRouletteResult {
        validateUser(command.userId)
        val participationDate = command.participationDate ?: LocalDate.now(ZoneOffset.UTC)
        rejectIfAlreadyParticipated(command.userId, participationDate)
        ensureDailyBudget(participationDate)

        val rewardPoints = rouletteRewardSelector.selectRewardPoints()
        allocateBudgetOrThrow(participationDate, rewardPoints)

        val awardedAt = LocalDateTime.now(ZoneOffset.UTC)
        val pointExpiresAt = awardedAt.plusDays(BusinessConstants.POINT_EXPIRATION_DAYS)
        val participationId = insertParticipationOrThrow(
            userId = command.userId,
            participationDate = participationDate,
            rewardPoints = rewardPoints,
            awardedAt = awardedAt,
            pointExpiresAt = pointExpiresAt,
        )

        pointUnitWriteRepository.insertPointUnit(
            userId = command.userId,
            eventType = PointEventType.ROULETTE_REWARD,
            amount = rewardPoints,
            earnedAt = awardedAt,
            expiresAt = pointExpiresAt,
            sourceParticipationId = participationId,
        )
        pointTransactionWriteRepository.insertTransaction(
            userId = command.userId,
            eventType = PointEventType.ROULETTE_REWARD,
            direction = PointTransactionDirection.CREDIT,
            amount = rewardPoints,
            occurredAt = awardedAt,
            participationId = participationId,
        )

        val updatedBudget = dailyBudgetReadRepository.findByBudgetDate(participationDate)
            ?: throw BusinessException("BUDGET_NOT_FOUND", "일일 예산 정보를 찾을 수 없습니다.")
        return ParticipateRouletteResult(
            participationId = participationId,
            participationDate = participationDate,
            awardedPoints = rewardPoints,
            pointExpiresAt = pointExpiresAt,
            remainingBudgetPoints = updatedBudget.remainingBudgetPoints(),
        )
    }

    private fun validateUser(userId: Long) {
        if (userReadRepository.existsById(userId)) return
        throw BusinessException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다. userId=$userId")
    }

    private fun rejectIfAlreadyParticipated(
        userId: Long,
        participationDate: LocalDate,
    ) {
        val alreadyParticipated = rouletteParticipationReadRepository
            .existsByUserIdAndParticipationDateAndCanceledFalse(userId, participationDate)
        if (!alreadyParticipated) return
        throw BusinessException("ROULETTE_ALREADY_PARTICIPATED_TODAY", "오늘은 이미 룰렛에 참여했습니다.")
    }

    private fun ensureDailyBudget(participationDate: LocalDate) {
        val existingDailyBudget = dailyBudgetReadRepository.findByBudgetDate(participationDate)
        if (existingDailyBudget != null) return
        try {
            dailyBudgetWriteRepository.insertDailyBudget(
                budgetDate = participationDate,
                totalBudgetPoints = BusinessConstants.DEFAULT_DAILY_BUDGET_POINTS,
            )
        } catch (_: DataIntegrityViolationException) {
            // 동시 요청으로 다른 트랜잭션이 같은 날짜 예산을 먼저 생성한 경우는 정상 흐름이다.
        }
    }

    private fun allocateBudgetOrThrow(
        participationDate: LocalDate,
        rewardPoints: Int,
    ) {
        val updatedRows = dailyBudgetWriteRepository.allocateBudgetIfPossible(participationDate, rewardPoints)
        if (updatedRows > 0) return
        throw BusinessException("BUDGET_INSUFFICIENT", "오늘 잔여 예산이 부족합니다.")
    }

    private fun insertParticipationOrThrow(
        userId: Long,
        participationDate: LocalDate,
        rewardPoints: Int,
        awardedAt: LocalDateTime,
        pointExpiresAt: LocalDateTime,
    ): Long {
        return try {
            rouletteParticipationWriteRepository.insertParticipation(
                userId = userId,
                participationDate = participationDate,
                awardedPoints = rewardPoints,
                awardedAt = awardedAt,
                pointExpiresAt = pointExpiresAt,
            )
        } catch (exception: DataIntegrityViolationException) {
            throw BusinessException(
                code = "ROULETTE_ALREADY_PARTICIPATED_TODAY",
                message = "동시 요청으로 이미 참여 처리되었습니다.",
            )
        }
    }
}

data class ParticipateRouletteCommand(
    val userId: Long,
    val participationDate: LocalDate? = null,
)

data class ParticipateRouletteResult(
    val participationId: Long,
    val participationDate: LocalDate,
    val awardedPoints: Int,
    val pointExpiresAt: LocalDateTime,
    val remainingBudgetPoints: Int,
)
