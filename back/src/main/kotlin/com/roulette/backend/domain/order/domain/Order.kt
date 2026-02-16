package com.roulette.backend.domain.order.domain

import com.roulette.backend.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.LocalDateTime
import java.time.ZoneOffset

@Entity
@Table(name = "orders")
class Order(
    userId: Long,
    productId: Long,
    quantity: Int,
    usedPoints: Int,
    orderedAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        protected set

    @Column(name = "product_id", nullable = false)
    var productId: Long = productId
        protected set

    @Column(name = "quantity", nullable = false)
    var quantity: Int = quantity
        protected set

    @Column(name = "used_points", nullable = false)
    var usedPoints: Int = usedPoints
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: OrderStatus = OrderStatus.PLACED
        protected set

    @Column(name = "ordered_at", nullable = false)
    var orderedAt: LocalDateTime = orderedAt
        protected set

    @Column(name = "canceled_at")
    var canceledAt: LocalDateTime? = null
        protected set

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        protected set

    init {
        validateIdentifier(userId, "사용자")
        validateIdentifier(productId, "상품")
        validateQuantity(quantity)
        validateUsedPoints(usedPoints)
    }

    fun cancel(cancelTime: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)) {
        require(status == OrderStatus.PLACED) { "이미 취소된 주문입니다." }
        status = OrderStatus.CANCELED
        canceledAt = cancelTime
    }

    private fun validateQuantity(value: Int) {
        require(value >= MIN_QUANTITY) { "주문 수량은 $MIN_QUANTITY 이상이어야 합니다." }
    }

    private fun validateUsedPoints(value: Int) {
        require(value >= MIN_USED_POINTS) { "사용 포인트는 $MIN_USED_POINTS 이상이어야 합니다." }
    }

    private fun validateIdentifier(identifier: Long, targetName: String) {
        require(identifier > 0) { "$targetName 식별자는 1 이상이어야 합니다." }
    }

    companion object {
        const val MIN_QUANTITY: Int = 1
        const val MIN_USED_POINTS: Int = 1
    }
}
