package com.roulette.backend.domain.roulette.domain

import com.roulette.backend.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * 유니크 정책 안내:
 * JPA 엔티티에서는 조건부 유니크 인덱스(WHERE is_canceled = FALSE)를 표현할 수 없어
 * 실제 제약은 DB 초기화 코드에서 관리한다.
 * 즉, 활성 참여(is_canceled = FALSE)끼리만 (user_id, participation_date) 유니크를 보장한다.
 */
@Entity
@Table(name = "roulette_participations")
class RouletteParticipation(
    userId: Long,
    participationDate: LocalDate,
    awardedPoints: Int,
    awardedAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
    pointExpiresAt: LocalDateTime,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        protected set

    @Column(name = "participation_date", nullable = false)
    var participationDate: LocalDate = participationDate
        protected set

    @Column(name = "awarded_points", nullable = false)
    var awardedPoints: Int = awardedPoints
        protected set

    @Column(name = "awarded_at", nullable = false)
    var awardedAt: LocalDateTime = awardedAt
        protected set

    @Column(name = "point_expires_at", nullable = false)
    var pointExpiresAt: LocalDateTime = pointExpiresAt
        protected set

    @Column(name = "is_canceled", nullable = false)
    var canceled: Boolean = false
        protected set

    @Column(name = "canceled_at")
    var canceledAt: LocalDateTime? = null
        protected set

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        protected set

    init {
        validateUserId(userId)
        validateAwardedPoint(awardedPoints)
    }

    fun cancel(cancelTime: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)) {
        require(!canceled) { "이미 취소된 룰렛 참여입니다." }
        canceled = true
        canceledAt = cancelTime
    }

    private fun validateAwardedPoint(points: Int) {
        require(points in RouletteRewardOption.allowedPoints) {
            "허용되지 않은 룰렛 포인트 값입니다. points=$points"
        }
    }

    private fun validateUserId(candidate: Long) {
        require(candidate > 0) { "사용자 식별자는 1 이상이어야 합니다." }
    }
}
