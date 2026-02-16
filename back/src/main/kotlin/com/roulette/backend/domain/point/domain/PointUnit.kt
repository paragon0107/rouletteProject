package com.roulette.backend.domain.point.domain

import com.roulette.backend.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.LocalDateTime
import java.time.ZoneOffset

@Entity
@Table(
    name = "point_units",
    indexes = [
        Index(name = "idx_point_units_user_id", columnList = "user_id"),
        Index(name = "idx_point_units_expires_at", columnList = "expires_at"),
    ],
)
class PointUnit(
    userId: Long,
    eventType: PointEventType,
    originalAmount: Int,
    expiresAt: LocalDateTime,
    sourceParticipationId: Long? = null,
    sourceOrderId: Long? = null,
    earnedAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    var eventType: PointEventType = eventType
        protected set

    @Column(name = "original_amount", nullable = false)
    var originalAmount: Int = originalAmount
        protected set

    @Column(name = "remaining_amount", nullable = false)
    var remainingAmount: Int = originalAmount
        protected set

    @Column(name = "earned_at", nullable = false)
    var earnedAt: LocalDateTime = earnedAt
        protected set

    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime = expiresAt
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: PointUnitStatus = PointUnitStatus.AVAILABLE
        protected set

    @Column(name = "source_participation_id")
    var sourceParticipationId: Long? = sourceParticipationId
        protected set

    @Column(name = "source_order_id")
    var sourceOrderId: Long? = sourceOrderId
        protected set

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        protected set

    init {
        validateUserId(userId)
        validateOriginalAmount(originalAmount)
    }

    fun use(amount: Int, referenceTime: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)) {
        validatePositiveAmount(amount)
        expireIfNeeded(referenceTime)
        require(status == PointUnitStatus.AVAILABLE) { "사용 가능한 포인트가 아닙니다." }
        require(remainingAmount >= amount) { "사용 포인트가 잔여 포인트를 초과할 수 없습니다." }
        remainingAmount -= amount
        if (remainingAmount == 0) status = PointUnitStatus.USED
    }

    fun refund(amount: Int) {
        validatePositiveAmount(amount)
        require(status != PointUnitStatus.CANCELED) { "회수된 포인트는 환불할 수 없습니다." }
        require(status != PointUnitStatus.EXPIRED) { "만료된 포인트는 환불할 수 없습니다." }
        require(remainingAmount + amount <= originalAmount) {
            "환불 포인트가 원본 포인트를 초과할 수 없습니다."
        }
        remainingAmount += amount
        if (remainingAmount > 0) status = PointUnitStatus.AVAILABLE
    }

    fun expire(referenceTime: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)) {
        if (status == PointUnitStatus.CANCELED) return
        if (referenceTime < expiresAt) return
        status = PointUnitStatus.EXPIRED
        remainingAmount = 0
    }

    fun cancel() {
        require(status != PointUnitStatus.CANCELED) { "이미 회수 처리된 포인트입니다." }
        status = PointUnitStatus.CANCELED
        remainingAmount = 0
    }

    fun isExpiringWithinDays(days: Long, referenceTime: LocalDateTime): Boolean {
        if (status != PointUnitStatus.AVAILABLE) return false
        if (days < 0) return false
        val thresholdTime = referenceTime.plusDays(days)
        return expiresAt <= thresholdTime
    }

    private fun expireIfNeeded(referenceTime: LocalDateTime) {
        if (referenceTime >= expiresAt && status == PointUnitStatus.AVAILABLE) {
            status = PointUnitStatus.EXPIRED
            remainingAmount = 0
        }
    }

    private fun validateOriginalAmount(amount: Int) {
        require(amount > 0) { "포인트 원본 금액은 1 이상이어야 합니다." }
    }

    private fun validatePositiveAmount(amount: Int) {
        require(amount > 0) { "포인트 금액은 1 이상이어야 합니다." }
    }

    private fun validateUserId(candidate: Long) {
        require(candidate > 0) { "사용자 식별자는 1 이상이어야 합니다." }
    }
}
