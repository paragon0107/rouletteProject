package com.roulette.backend.domain.point.repository

import com.roulette.backend.domain.point.domain.PointTransaction
import org.springframework.data.jpa.repository.JpaRepository

interface PointTransactionReadRepository : JpaRepository<PointTransaction, Long> {
    fun findAllByUserIdOrderByOccurredAtDesc(userId: Long): List<PointTransaction>
}
