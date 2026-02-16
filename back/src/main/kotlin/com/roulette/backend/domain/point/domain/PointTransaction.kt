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
import java.time.LocalDateTime
import java.time.ZoneOffset

@Entity
@Table(
    name = "point_transactions",
    indexes = [
        Index(name = "idx_point_transactions_user_id", columnList = "user_id"),
        Index(name = "idx_point_transactions_occurred_at", columnList = "occurred_at"),
    ],
)
class PointTransaction(
    userId: Long,
    eventType: PointEventType,
    direction: PointTransactionDirection,
    amount: Int,
    occurredAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
    orderId: Long? = null,
    participationId: Long? = null,
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

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 10)
    var direction: PointTransactionDirection = direction
        protected set

    @Column(name = "amount", nullable = false)
    var amount: Int = amount
        protected set

    @Column(name = "occurred_at", nullable = false)
    var occurredAt: LocalDateTime = occurredAt
        protected set

    @Column(name = "order_id")
    var orderId: Long? = orderId
        protected set

    @Column(name = "participation_id")
    var participationId: Long? = participationId
        protected set

    init {
        require(userId > 0) { "사용자 식별자는 1 이상이어야 합니다." }
        require(amount > 0) { "포인트 이력 금액은 1 이상이어야 합니다." }
    }
}
