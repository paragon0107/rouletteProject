package com.roulette.backend.domain.point.dto

import com.roulette.backend.domain.point.domain.PointEventType
import com.roulette.backend.domain.point.domain.PointUnitStatus
import com.roulette.backend.domain.point.service.ExpiringPointItemResult
import com.roulette.backend.domain.point.service.ExpiringPointsQueryResult
import com.roulette.backend.domain.point.service.MyPointUnitItemResult
import com.roulette.backend.domain.point.service.MyPointUnitListResult
import com.roulette.backend.domain.point.service.PointBalanceQueryResult
import java.time.LocalDateTime

data class PointUnitListResponse(
    val totalItems: Int,
    val items: List<PointUnitItemResponse>,
)

data class PointUnitItemResponse(
    val pointUnitId: Long,
    val eventType: PointEventType,
    val originalAmount: Int,
    val remainingAmount: Int,
    val earnedAt: LocalDateTime,
    val expiresAt: LocalDateTime,
    val status: PointUnitStatus,
)

data class PointBalanceResponse(
    val availableBalance: Int,
    val expiringWithin7Days: Int,
)

data class ExpiringPointsResponse(
    val withinDays: Long,
    val totalExpiringAmount: Int,
    val items: List<ExpiringPointItemResponse>,
)

data class ExpiringPointItemResponse(
    val pointUnitId: Long,
    val amount: Int,
    val expiresAt: LocalDateTime,
)

fun MyPointUnitListResult.toResponse(): PointUnitListResponse {
    return PointUnitListResponse(
        totalItems = totalItems,
        items = items.map { it.toResponse() },
    )
}

fun MyPointUnitItemResult.toResponse(): PointUnitItemResponse {
    return PointUnitItemResponse(
        pointUnitId = pointUnitId,
        eventType = eventType,
        originalAmount = originalAmount,
        remainingAmount = remainingAmount,
        earnedAt = earnedAt,
        expiresAt = expiresAt,
        status = status,
    )
}

fun PointBalanceQueryResult.toResponse(): PointBalanceResponse {
    return PointBalanceResponse(
        availableBalance = availableBalance,
        expiringWithin7Days = expiringSoonAmount,
    )
}

fun ExpiringPointsQueryResult.toResponse(): ExpiringPointsResponse {
    return ExpiringPointsResponse(
        withinDays = withinDays,
        totalExpiringAmount = totalExpiringAmount,
        items = items.map { it.toResponse() },
    )
}

fun ExpiringPointItemResult.toResponse(): ExpiringPointItemResponse {
    return ExpiringPointItemResponse(
        pointUnitId = pointUnitId,
        amount = amount,
        expiresAt = expiresAt,
    )
}
