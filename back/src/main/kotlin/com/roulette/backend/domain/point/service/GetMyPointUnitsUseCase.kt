package com.roulette.backend.domain.point.service

import com.roulette.backend.domain.point.domain.PointEventType
import com.roulette.backend.domain.point.domain.PointUnit
import com.roulette.backend.domain.point.domain.PointUnitStatus
import com.roulette.backend.domain.point.repository.PointUnitReadRepository
import com.roulette.backend.domain.point.repository.PointUnitWriteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class GetMyPointUnitsUseCase(
    private val pointUnitReadRepository: PointUnitReadRepository,
    private val pointUnitWriteRepository: PointUnitWriteRepository,
) {
    @Transactional
    fun execute(userId: Long): MyPointUnitListResult {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        pointUnitWriteRepository.expirePointUnits(now)
        val pointUnits = pointUnitReadRepository.findAllByUserIdOrderByEarnedAtDesc(userId)
        val items = pointUnits.map { pointUnit -> pointUnit.toItemResult() }
        return MyPointUnitListResult(totalItems = items.size, items = items)
    }
}

data class MyPointUnitListResult(
    val totalItems: Int,
    val items: List<MyPointUnitItemResult>,
)

data class MyPointUnitItemResult(
    val pointUnitId: Long,
    val eventType: PointEventType,
    val originalAmount: Int,
    val remainingAmount: Int,
    val earnedAt: LocalDateTime,
    val expiresAt: LocalDateTime,
    val status: PointUnitStatus,
)

private fun PointUnit.toItemResult(): MyPointUnitItemResult {
    return MyPointUnitItemResult(
        pointUnitId = requireNotNull(id),
        eventType = eventType,
        originalAmount = originalAmount,
        remainingAmount = remainingAmount,
        earnedAt = earnedAt,
        expiresAt = expiresAt,
        status = status,
    )
}
