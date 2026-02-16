package com.roulette.backend.domain.order.service

import com.roulette.backend.common.exception.BusinessException
import com.roulette.backend.common.util.BusinessConstants
import com.roulette.backend.domain.order.domain.OrderStatus
import com.roulette.backend.domain.order.repository.OrderReadRepository
import com.roulette.backend.domain.order.repository.OrderWriteRepository
import com.roulette.backend.domain.point.domain.PointEventType
import com.roulette.backend.domain.point.domain.PointTransactionDirection
import com.roulette.backend.domain.point.repository.PointTransactionWriteRepository
import com.roulette.backend.domain.point.repository.PointUnitWriteRepository
import com.roulette.backend.domain.product.repository.ProductWriteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class CancelOrderUseCase(
    private val orderReadRepository: OrderReadRepository,
    private val orderWriteRepository: OrderWriteRepository,
    private val productWriteRepository: ProductWriteRepository,
    private val pointUnitWriteRepository: PointUnitWriteRepository,
    private val pointTransactionWriteRepository: PointTransactionWriteRepository,
) {
    @Transactional
    fun execute(orderId: Long): CancelOrderResult {
        val order = orderReadRepository.findById(orderId)
            .orElseThrow { BusinessException("ORDER_NOT_FOUND", "주문을 찾을 수 없습니다.") }
        if (order.status == OrderStatus.CANCELED) {
            throw BusinessException("ORDER_ALREADY_CANCELED", "이미 취소된 주문입니다.")
        }
        if (order.status != OrderStatus.PLACED) {
            throw BusinessException("ORDER_CANCEL_NOT_ALLOWED", "주문 취소는 PLACED 상태에서만 가능합니다.")
        }

        cancelOrderOrThrow(orderId)
        restoreStock(order.productId, order.quantity)
        val refundedAt = LocalDateTime.now(ZoneOffset.UTC)
        val expiresAt = refundedAt.plusDays(BusinessConstants.POINT_EXPIRATION_DAYS)
        pointUnitWriteRepository.insertPointUnit(
            userId = order.userId,
            eventType = PointEventType.ORDER_REFUND,
            amount = order.usedPoints,
            earnedAt = refundedAt,
            expiresAt = expiresAt,
            sourceOrderId = orderId,
        )
        pointTransactionWriteRepository.insertTransaction(
            userId = order.userId,
            eventType = PointEventType.ORDER_REFUND,
            direction = PointTransactionDirection.CREDIT,
            amount = order.usedPoints,
            occurredAt = refundedAt,
            orderId = orderId,
        )
        return CancelOrderResult(
            orderId = orderId,
            userId = order.userId,
            refundedPoints = order.usedPoints,
            refundedAt = refundedAt,
        )
    }

    private fun cancelOrderOrThrow(orderId: Long) {
        val updatedRows = orderWriteRepository.cancelOrder(orderId)
        if (updatedRows > 0) return
        throw BusinessException("ORDER_CANCEL_NOT_ALLOWED", "주문 상태가 변경되어 취소할 수 없습니다.")
    }

    private fun restoreStock(
        productId: Long,
        quantity: Int,
    ) {
        val updatedRows = productWriteRepository.increaseStock(productId, quantity)
        if (updatedRows > 0) return
        throw BusinessException("PRODUCT_STOCK_RESTORE_FAILED", "재고 복구에 실패했습니다.")
    }
}

data class CancelOrderResult(
    val orderId: Long,
    val userId: Long,
    val refundedPoints: Int,
    val refundedAt: LocalDateTime,
)
