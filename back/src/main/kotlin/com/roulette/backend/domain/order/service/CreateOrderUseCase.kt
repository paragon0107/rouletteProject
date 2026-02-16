package com.roulette.backend.domain.order.service

import com.roulette.backend.common.exception.BusinessException
import com.roulette.backend.domain.order.repository.OrderWriteRepository
import com.roulette.backend.domain.point.domain.PointEventType
import com.roulette.backend.domain.point.domain.PointTransactionDirection
import com.roulette.backend.domain.point.domain.PointUnitStatus
import com.roulette.backend.domain.point.repository.PointTransactionWriteRepository
import com.roulette.backend.domain.point.repository.PointUnitReadRepository
import com.roulette.backend.domain.point.repository.PointUnitWriteRepository
import com.roulette.backend.domain.product.domain.ProductStatus
import com.roulette.backend.domain.product.repository.ProductReadRepository
import com.roulette.backend.domain.product.repository.ProductWriteRepository
import com.roulette.backend.domain.user.repository.UserReadRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class CreateOrderUseCase(
    private val userReadRepository: UserReadRepository,
    private val productReadRepository: ProductReadRepository,
    private val productWriteRepository: ProductWriteRepository,
    private val orderWriteRepository: OrderWriteRepository,
    private val pointUnitReadRepository: PointUnitReadRepository,
    private val pointUnitWriteRepository: PointUnitWriteRepository,
    private val pointTransactionWriteRepository: PointTransactionWriteRepository,
) {
    @Transactional
    fun execute(command: CreateOrderCommand): CreateOrderResult {
        validateCommand(command)
        validateUser(command.userId)
        val product = productReadRepository.findById(command.productId)
            .orElseThrow { BusinessException("PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다.") }
        if (product.status != ProductStatus.ACTIVE) {
            throw BusinessException("PRODUCT_INACTIVE", "비활성 상품은 주문할 수 없습니다.")
        }

        val requiredPoints = product.pricePoints * command.quantity
        val orderedAt = LocalDateTime.now(ZoneOffset.UTC)
        pointUnitWriteRepository.expirePointUnits(orderedAt)
        rejectIfInsufficientBalance(command.userId, requiredPoints, orderedAt)
        decreaseStockOrThrow(command.productId, command.quantity)

        val orderId = orderWriteRepository.insertOrder(
            userId = command.userId,
            productId = command.productId,
            quantity = command.quantity,
            usedPoints = requiredPoints,
            orderedAt = orderedAt,
        )
        deductPointsFromOldestUnits(command.userId, orderId, requiredPoints, orderedAt)
        return CreateOrderResult(
            orderId = orderId,
            userId = command.userId,
            productId = command.productId,
            quantity = command.quantity,
            usedPoints = requiredPoints,
            orderedAt = orderedAt,
        )
    }

    private fun validateCommand(command: CreateOrderCommand) {
        if (command.userId > 0 && command.productId > 0 && command.quantity > 0) return
        throw BusinessException("ORDER_INVALID_REQUEST", "주문 파라미터가 유효하지 않습니다.")
    }

    private fun validateUser(userId: Long) {
        if (userReadRepository.existsById(userId)) return
        throw BusinessException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다. userId=$userId")
    }

    private fun rejectIfInsufficientBalance(
        userId: Long,
        requiredPoints: Int,
        referenceTime: LocalDateTime,
    ) {
        val availableBalance = pointUnitReadRepository.sumAvailableBalance(userId, referenceTime).toInt()
        if (availableBalance >= requiredPoints) return
        throw BusinessException("POINT_BALANCE_INSUFFICIENT", "보유 포인트가 부족합니다.")
    }

    private fun decreaseStockOrThrow(
        productId: Long,
        quantity: Int,
    ) {
        val updatedRows = productWriteRepository.decreaseStockIfPossible(productId, quantity)
        if (updatedRows > 0) return
        throw BusinessException("PRODUCT_STOCK_INSUFFICIENT", "재고가 부족하거나 주문 불가능한 상태입니다.")
    }

    private fun deductPointsFromOldestUnits(
        userId: Long,
        orderId: Long,
        requiredPoints: Int,
        orderedAt: LocalDateTime,
    ) {
        var remainingPoints = requiredPoints
        val availableUnits = pointUnitReadRepository.findAllByUserIdAndStatusOrderByExpiresAtAsc(
            userId = userId,
            status = PointUnitStatus.AVAILABLE,
        )
        availableUnits.forEach { pointUnit ->
            if (remainingPoints == 0) return@forEach
            val pointUnitId = requireNotNull(pointUnit.id) { "포인트 단위 식별자가 없습니다." }
            val deductAmount = minOf(pointUnit.remainingAmount, remainingPoints)
            if (deductAmount == 0) return@forEach
            val updatedRows = pointUnitWriteRepository.deductAmountIfPossible(pointUnitId, deductAmount)
            if (updatedRows == 0) {
                throw BusinessException("POINT_DEDUCTION_CONFLICT", "포인트 차감 중 충돌이 발생했습니다.")
            }
            pointTransactionWriteRepository.insertTransaction(
                userId = userId,
                eventType = PointEventType.ORDER_USE,
                direction = PointTransactionDirection.DEBIT,
                amount = deductAmount,
                occurredAt = orderedAt,
                orderId = orderId,
            )
            remainingPoints -= deductAmount
        }
        if (remainingPoints == 0) return
        throw BusinessException("POINT_BALANCE_INSUFFICIENT", "포인트 차감에 실패했습니다.")
    }
}

data class CreateOrderCommand(
    val userId: Long,
    val productId: Long,
    val quantity: Int,
)

data class CreateOrderResult(
    val orderId: Long,
    val userId: Long,
    val productId: Long,
    val quantity: Int,
    val usedPoints: Int,
    val orderedAt: LocalDateTime,
)
