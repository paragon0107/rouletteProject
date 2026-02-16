package com.roulette.backend.domain.order.service

import com.roulette.backend.common.exception.BusinessException
import com.roulette.backend.domain.order.domain.Order
import com.roulette.backend.domain.order.domain.OrderStatus
import com.roulette.backend.domain.order.repository.OrderReadRepository
import com.roulette.backend.domain.order.repository.OrderWriteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateOrderStatusUseCase(
    private val orderReadRepository: OrderReadRepository,
    private val orderWriteRepository: OrderWriteRepository,
) {
    @Transactional
    fun execute(
        orderId: Long,
        newStatus: OrderStatus,
    ): AdminOrderItemResult {
        rejectIfCancelStatus(newStatus)
        val order = findOrder(orderId)
        rejectIfCanceled(order)
        if (order.status == newStatus) return order.toAdminItemResult()

        val updatedRows = orderWriteRepository.updateStatusIfPossible(orderId, newStatus)
        if (updatedRows > 0) return findOrder(orderId).toAdminItemResult()
        throw BusinessException("ORDER_STATUS_CHANGE_NOT_ALLOWED", "주문 상태 변경 중 충돌이 발생했습니다.")
    }

    private fun rejectIfCancelStatus(newStatus: OrderStatus) {
        if (newStatus != OrderStatus.CANCELED) return
        throw BusinessException("ORDER_STATUS_CHANGE_NOT_ALLOWED", "주문 취소는 전용 취소 API를 사용해야 합니다.")
    }

    private fun findOrder(orderId: Long): Order {
        return orderReadRepository.findById(orderId)
            .orElseThrow { BusinessException("ORDER_NOT_FOUND", "주문을 찾을 수 없습니다.") }
    }

    private fun rejectIfCanceled(order: Order) {
        if (order.status != OrderStatus.CANCELED) return
        throw BusinessException("ORDER_STATUS_CHANGE_NOT_ALLOWED", "취소된 주문의 상태는 변경할 수 없습니다.")
    }
}
