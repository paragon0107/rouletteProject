package com.roulette.backend.domain.point.repository

import com.roulette.backend.domain.point.domain.PointUnit
import com.roulette.backend.domain.point.domain.PointUnitStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface PointUnitReadRepository : JpaRepository<PointUnit, Long> {
    fun findAllByUserIdOrderByEarnedAtDesc(userId: Long): List<PointUnit>

    fun findAllByUserIdAndStatusOrderByExpiresAtAsc(
        userId: Long,
        status: PointUnitStatus,
    ): List<PointUnit>

    fun findAllByUserIdAndStatusAndExpiresAtLessThanEqualOrderByExpiresAtAsc(
        userId: Long,
        status: PointUnitStatus,
        expiresAt: LocalDateTime,
    ): List<PointUnit>

    fun findAllBySourceParticipationId(sourceParticipationId: Long): List<PointUnit>

    @Query(
        """
        SELECT COALESCE(SUM(p.remainingAmount), 0)
        FROM PointUnit p
        WHERE p.userId = :userId
          AND p.status = com.roulette.backend.domain.point.domain.PointUnitStatus.AVAILABLE
          AND p.expiresAt > :referenceTime
        """,
    )
    fun sumAvailableBalance(
        @Param("userId") userId: Long,
        @Param("referenceTime") referenceTime: LocalDateTime,
    ): Long
}
