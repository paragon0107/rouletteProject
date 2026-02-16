package com.roulette.backend.domain.point.service

import com.roulette.backend.domain.point.domain.PointUnitStatus
import com.roulette.backend.domain.point.repository.PointUnitReadRepository
import com.roulette.backend.domain.point.repository.PointUnitWriteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class GetPointBalanceUseCase(
    private val pointUnitReadRepository: PointUnitReadRepository,
    private val pointUnitWriteRepository: PointUnitWriteRepository,
) {
    @Transactional
    fun execute(userId: Long): PointBalanceQueryResult {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        pointUnitWriteRepository.expirePointUnits(now)
        val availableBalance = pointUnitReadRepository.sumAvailableBalance(userId, now).toInt()
        val expiringSoonThreshold = now.plusDays(EXPIRING_SOON_DAYS)
        val expiringSoonAmount = pointUnitReadRepository
            .findAllByUserIdAndStatusAndExpiresAtLessThanEqualOrderByExpiresAtAsc(
                userId = userId,
                status = PointUnitStatus.AVAILABLE,
                expiresAt = expiringSoonThreshold,
            )
            .sumOf { it.remainingAmount }
        return PointBalanceQueryResult(
            userId = userId,
            availableBalance = availableBalance,
            expiringSoonAmount = expiringSoonAmount,
        )
    }

    companion object {
        private const val EXPIRING_SOON_DAYS = 7L
    }
}

data class PointBalanceQueryResult(
    val userId: Long,
    val availableBalance: Int,
    val expiringSoonAmount: Int,
)
