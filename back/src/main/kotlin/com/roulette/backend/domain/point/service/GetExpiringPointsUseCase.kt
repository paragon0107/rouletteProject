package com.roulette.backend.domain.point.service

import com.roulette.backend.common.exception.BusinessException
import com.roulette.backend.domain.point.domain.PointUnitStatus
import com.roulette.backend.domain.point.repository.PointUnitReadRepository
import com.roulette.backend.domain.point.repository.PointUnitWriteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class GetExpiringPointsUseCase(
    private val pointUnitReadRepository: PointUnitReadRepository,
    private val pointUnitWriteRepository: PointUnitWriteRepository,
) {
    @Transactional
    fun execute(
        userId: Long,
        withinDays: Long = DEFAULT_WITHIN_DAYS,
    ): ExpiringPointsQueryResult {
        if (withinDays < 1) {
            throw BusinessException("POINT_INVALID_WITHIN_DAYS", "withinDays는 1 이상이어야 합니다.")
        }
        val now = LocalDateTime.now(ZoneOffset.UTC)
        pointUnitWriteRepository.expirePointUnits(now)
        val thresholdTime = now.plusDays(withinDays)
        val expiringPointUnits = pointUnitReadRepository
            .findAllByUserIdAndStatusAndExpiresAtLessThanEqualOrderByExpiresAtAsc(
                userId = userId,
                status = PointUnitStatus.AVAILABLE,
                expiresAt = thresholdTime,
            )
        val itemResults = expiringPointUnits.map { pointUnit ->
            ExpiringPointItemResult(
                pointUnitId = requireNotNull(pointUnit.id) { "포인트 단위 식별자가 없습니다." },
                amount = pointUnit.remainingAmount,
                expiresAt = pointUnit.expiresAt,
            )
        }
        return ExpiringPointsQueryResult(
            userId = userId,
            withinDays = withinDays,
            totalExpiringAmount = itemResults.sumOf { it.amount },
            items = itemResults,
        )
    }

    companion object {
        private const val DEFAULT_WITHIN_DAYS = 7L
    }
}

data class ExpiringPointsQueryResult(
    val userId: Long,
    val withinDays: Long,
    val totalExpiringAmount: Int,
    val items: List<ExpiringPointItemResult>,
)

data class ExpiringPointItemResult(
    val pointUnitId: Long,
    val amount: Int,
    val expiresAt: LocalDateTime,
)
