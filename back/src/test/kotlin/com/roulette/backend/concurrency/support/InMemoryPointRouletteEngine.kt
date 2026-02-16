package com.roulette.backend.concurrency.support

import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

enum class FailureCode {
    ROULETTE_ALREADY_PARTICIPATED_TODAY,
    BUDGET_INSUFFICIENT,
    BUDGET_TOTAL_LESS_THAN_USED,
    POINT_BALANCE_INSUFFICIENT,
    POINT_ALREADY_USED,
    PRODUCT_NOT_FOUND,
    PRODUCT_STOCK_INSUFFICIENT,
    ORDER_NOT_FOUND,
    ORDER_ALREADY_CANCELED,
    ROULETTE_PARTICIPATION_NOT_FOUND,
    ROULETTE_PARTICIPATION_ALREADY_CANCELED,
    INVALID_REQUEST,
}

class ConcurrencyConflictException(
    val failureCode: FailureCode,
    override val message: String,
) : RuntimeException(message)

enum class OrderStatus {
    PLACED,
    CANCELED,
}

data class Participation(
    val participationId: Long,
    val userId: Long,
    val participationDate: LocalDate,
    val rewardPoints: Int,
    var canceled: Boolean = false,
)

data class Product(
    val productId: Long,
    val name: String,
    val pricePoints: Int,
    var stock: Int,
)

data class Order(
    val orderId: Long,
    val userId: Long,
    val productId: Long,
    val quantity: Int,
    val usedPoints: Int,
    var status: OrderStatus = OrderStatus.PLACED,
)

data class EngineSnapshot(
    val totalBudget: Int,
    val usedBudget: Int,
    val remainingBudget: Int,
    val activeParticipationCount: Int,
    val totalParticipationCount: Int,
    val placedOrderCount: Int,
    val canceledOrderCount: Int,
)

class InMemoryPointRouletteEngine(
    initialDailyBudget: Int,
    private val rouletteRewardPoints: Int = DEFAULT_ROULETTE_REWARD_POINTS,
    private val operationDate: LocalDate = DEFAULT_OPERATION_DATE,
) {
    private val stateLock = ReentrantLock(true)

    private var totalBudget: Int = initialDailyBudget
    private var usedBudget: Int = 0

    private val nextParticipationId = AtomicLong(1)
    private val nextOrderId = AtomicLong(1)

    private val participationByUserId = linkedMapOf<Long, Participation>()
    private val participationById = linkedMapOf<Long, Participation>()
    private val productById = linkedMapOf<Long, Product>()
    private val orderById = linkedMapOf<Long, Order>()
    private val pointBalanceByUserId = linkedMapOf<Long, Int>()

    init {
        require(initialDailyBudget >= 0) { "초기 예산은 0 이상이어야 합니다." }
        require(rouletteRewardPoints > 0) { "룰렛 보상은 1 이상이어야 합니다." }
    }

    fun registerProduct(
        productId: Long,
        name: String,
        pricePoints: Int,
        stock: Int,
    ) {
        validateProduct(productId, name, pricePoints, stock)
        stateLock.withLock {
            productById[productId] = Product(productId, name, pricePoints, stock)
        }
    }

    fun participate(userId: Long): Participation {
        validateUserId(userId)
        return stateLock.withLock {
            rejectIfAlreadyParticipatedToday(userId)
            rejectIfBudgetInsufficient(rouletteRewardPoints)
            val participation = createParticipation(userId)
            saveParticipation(participation)
            usedBudget += rouletteRewardPoints
            addPoint(userId, rouletteRewardPoints)
            participation
        }
    }

    fun order(
        userId: Long,
        productId: Long,
        quantity: Int,
    ): Order {
        validateUserId(userId)
        validateQuantity(quantity)
        return stateLock.withLock {
            val product = findProduct(productId)
            val requiredPoints = product.pricePoints * quantity
            rejectIfStockInsufficient(product, quantity)
            rejectIfPointInsufficient(userId, requiredPoints)
            product.stock -= quantity
            subtractPoint(userId, requiredPoints)
            val order = createOrder(userId, productId, quantity, requiredPoints)
            orderById[order.orderId] = order
            order
        }
    }

    fun cancelOrderByAdmin(orderId: Long): Order {
        return stateLock.withLock {
            val order = findOrder(orderId)
            rejectIfOrderAlreadyCanceled(order)
            val product = findProduct(order.productId)
            order.status = OrderStatus.CANCELED
            product.stock += order.quantity
            addPoint(order.userId, order.usedPoints)
            order
        }
    }

    fun cancelParticipationByAdmin(participationId: Long): Participation {
        return stateLock.withLock {
            val participation = findParticipation(participationId)
            rejectIfParticipationAlreadyCanceled(participation)
            rejectIfAwardedPointAlreadyUsed(participation)
            participation.canceled = true
            usedBudget -= participation.rewardPoints
            subtractPoint(participation.userId, participation.rewardPoints)
            participation
        }
    }

    fun updateDailyBudgetByAdmin(newTotalBudget: Int) {
        if (newTotalBudget < 0) {
            throw ConcurrencyConflictException(
                failureCode = FailureCode.INVALID_REQUEST,
                message = "총 예산은 0 이상이어야 합니다.",
            )
        }
        stateLock.withLock {
            if (newTotalBudget < usedBudget) {
                throw ConcurrencyConflictException(
                    failureCode = FailureCode.BUDGET_TOTAL_LESS_THAN_USED,
                    message = "총 예산은 현재 사용 예산보다 작을 수 없습니다.",
                )
            }
            totalBudget = newTotalBudget
        }
    }

    fun userPointBalance(userId: Long): Int {
        validateUserId(userId)
        return stateLock.withLock { currentPoint(userId) }
    }

    fun productStock(productId: Long): Int {
        return stateLock.withLock { findProduct(productId).stock }
    }

    fun participationIds(): List<Long> {
        return stateLock.withLock { participationById.keys.toList() }
    }

    fun placedOrderIds(): List<Long> {
        return stateLock.withLock {
            orderById.values
                .filter { it.status == OrderStatus.PLACED }
                .map { it.orderId }
        }
    }

    fun firstParticipationIdOrNull(): Long? {
        return stateLock.withLock { participationById.keys.firstOrNull() }
    }

    fun firstPlacedOrderIdOrNull(): Long? {
        return stateLock.withLock {
            orderById.values
                .firstOrNull { it.status == OrderStatus.PLACED }
                ?.orderId
        }
    }

    fun snapshot(): EngineSnapshot {
        return stateLock.withLock {
            val activeParticipationCount = participationById.values.count { !it.canceled }
            val placedOrderCount = orderById.values.count { it.status == OrderStatus.PLACED }
            val canceledOrderCount = orderById.values.count { it.status == OrderStatus.CANCELED }
            EngineSnapshot(
                totalBudget = totalBudget,
                usedBudget = usedBudget,
                remainingBudget = totalBudget - usedBudget,
                activeParticipationCount = activeParticipationCount,
                totalParticipationCount = participationById.size,
                placedOrderCount = placedOrderCount,
                canceledOrderCount = canceledOrderCount,
            )
        }
    }

    fun assertInternalConsistency() {
        stateLock.withLock {
            assertBudgetRange()
            assertUsedBudgetConsistency()
            assertPointBalanceConsistency()
            assertProductStockConsistency()
        }
    }

    private fun assertBudgetRange() {
        require(totalBudget >= 0) { "총 예산은 음수가 될 수 없습니다." }
        require(usedBudget >= 0) { "사용 예산은 음수가 될 수 없습니다." }
        require(usedBudget <= totalBudget) { "사용 예산이 총 예산을 초과했습니다." }
    }

    private fun assertUsedBudgetConsistency() {
        val expectedUsedBudget = participationById.values
            .filter { !it.canceled }
            .sumOf { it.rewardPoints }
        require(expectedUsedBudget == usedBudget) {
            "사용 예산 불일치: expected=$expectedUsedBudget, actual=$usedBudget"
        }
    }

    private fun assertPointBalanceConsistency() {
        val expectedPointByUserId = mutableMapOf<Long, Int>()
        participationById.values
            .filter { !it.canceled }
            .forEach { expectedPointByUserId.addToPoint(it.userId, it.rewardPoints) }
        orderById.values
            .filter { it.status == OrderStatus.PLACED }
            .forEach { expectedPointByUserId.addToPoint(it.userId, -it.usedPoints) }

        val userIds = expectedPointByUserId.keys + pointBalanceByUserId.keys
        userIds.forEach { userId ->
            val expected = expectedPointByUserId[userId] ?: 0
            val actual = pointBalanceByUserId[userId] ?: 0
            require(actual >= 0) { "포인트는 음수가 될 수 없습니다. userId=$userId, actual=$actual" }
            require(expected == actual) {
                "포인트 불일치: userId=$userId, expected=$expected, actual=$actual"
            }
        }
    }

    private fun assertProductStockConsistency() {
        productById.values.forEach { product ->
            require(product.stock >= 0) {
                "상품 재고는 음수가 될 수 없습니다. productId=${product.productId}"
            }
        }
    }

    private fun MutableMap<Long, Int>.addToPoint(
        userId: Long,
        amount: Int,
    ) {
        val current = this[userId] ?: 0
        this[userId] = current + amount
    }

    private fun validateProduct(
        productId: Long,
        name: String,
        pricePoints: Int,
        stock: Int,
    ) {
        if (productId <= 0) {
            throw ConcurrencyConflictException(FailureCode.INVALID_REQUEST, "상품 식별자는 1 이상이어야 합니다.")
        }
        if (name.isBlank()) {
            throw ConcurrencyConflictException(FailureCode.INVALID_REQUEST, "상품명은 비어 있을 수 없습니다.")
        }
        if (pricePoints <= 0 || stock < 0) {
            throw ConcurrencyConflictException(FailureCode.INVALID_REQUEST, "가격/재고 값이 유효하지 않습니다.")
        }
    }

    private fun validateUserId(userId: Long) {
        if (userId <= 0) {
            throw ConcurrencyConflictException(
                failureCode = FailureCode.INVALID_REQUEST,
                message = "사용자 식별자는 1 이상이어야 합니다.",
            )
        }
    }

    private fun validateQuantity(quantity: Int) {
        if (quantity <= 0) {
            throw ConcurrencyConflictException(
                failureCode = FailureCode.INVALID_REQUEST,
                message = "주문 수량은 1 이상이어야 합니다.",
            )
        }
    }

    private fun rejectIfAlreadyParticipatedToday(userId: Long) {
        val existingParticipation = participationByUserId[userId]
        if (existingParticipation != null && !existingParticipation.canceled) {
            throw ConcurrencyConflictException(
                failureCode = FailureCode.ROULETTE_ALREADY_PARTICIPATED_TODAY,
                message = "오늘은 이미 룰렛에 참여했습니다.",
            )
        }
    }

    private fun rejectIfBudgetInsufficient(requestedPoints: Int) {
        if (usedBudget + requestedPoints > totalBudget) {
            throw ConcurrencyConflictException(
                failureCode = FailureCode.BUDGET_INSUFFICIENT,
                message = "오늘 잔여 예산이 부족합니다.",
            )
        }
    }

    private fun rejectIfPointInsufficient(
        userId: Long,
        requestedPoints: Int,
    ) {
        if (currentPoint(userId) < requestedPoints) {
            throw ConcurrencyConflictException(
                failureCode = FailureCode.POINT_BALANCE_INSUFFICIENT,
                message = "보유 포인트가 부족합니다.",
            )
        }
    }

    private fun rejectIfStockInsufficient(
        product: Product,
        quantity: Int,
    ) {
        if (product.stock < quantity) {
            throw ConcurrencyConflictException(
                failureCode = FailureCode.PRODUCT_STOCK_INSUFFICIENT,
                message = "재고가 부족합니다.",
            )
        }
    }

    private fun rejectIfOrderAlreadyCanceled(order: Order) {
        if (order.status == OrderStatus.CANCELED) {
            throw ConcurrencyConflictException(
                failureCode = FailureCode.ORDER_ALREADY_CANCELED,
                message = "이미 취소된 주문입니다.",
            )
        }
    }

    private fun rejectIfParticipationAlreadyCanceled(participation: Participation) {
        if (participation.canceled) {
            throw ConcurrencyConflictException(
                failureCode = FailureCode.ROULETTE_PARTICIPATION_ALREADY_CANCELED,
                message = "이미 취소된 룰렛 참여입니다.",
            )
        }
    }

    private fun rejectIfAwardedPointAlreadyUsed(participation: Participation) {
        if (currentPoint(participation.userId) < participation.rewardPoints) {
            throw ConcurrencyConflictException(
                failureCode = FailureCode.POINT_ALREADY_USED,
                message = "지급 포인트가 이미 사용되어 회수할 수 없습니다.",
            )
        }
    }

    private fun createParticipation(userId: Long): Participation {
        return Participation(
            participationId = nextParticipationId.getAndIncrement(),
            userId = userId,
            participationDate = operationDate,
            rewardPoints = rouletteRewardPoints,
        )
    }

    private fun saveParticipation(participation: Participation) {
        participationByUserId[participation.userId] = participation
        participationById[participation.participationId] = participation
    }

    private fun createOrder(
        userId: Long,
        productId: Long,
        quantity: Int,
        usedPoints: Int,
    ): Order {
        return Order(
            orderId = nextOrderId.getAndIncrement(),
            userId = userId,
            productId = productId,
            quantity = quantity,
            usedPoints = usedPoints,
        )
    }

    private fun findProduct(productId: Long): Product {
        return productById[productId]
            ?: throw ConcurrencyConflictException(
                failureCode = FailureCode.PRODUCT_NOT_FOUND,
                message = "상품을 찾을 수 없습니다. productId=$productId",
            )
    }

    private fun findOrder(orderId: Long): Order {
        return orderById[orderId]
            ?: throw ConcurrencyConflictException(
                failureCode = FailureCode.ORDER_NOT_FOUND,
                message = "주문을 찾을 수 없습니다. orderId=$orderId",
            )
    }

    private fun findParticipation(participationId: Long): Participation {
        return participationById[participationId]
            ?: throw ConcurrencyConflictException(
                failureCode = FailureCode.ROULETTE_PARTICIPATION_NOT_FOUND,
                message = "룰렛 참여를 찾을 수 없습니다. participationId=$participationId",
            )
    }

    private fun currentPoint(userId: Long): Int {
        return pointBalanceByUserId[userId] ?: 0
    }

    private fun addPoint(
        userId: Long,
        amount: Int,
    ) {
        val nextPoint = currentPoint(userId) + amount
        pointBalanceByUserId[userId] = nextPoint
    }

    private fun subtractPoint(
        userId: Long,
        amount: Int,
    ) {
        val nextPoint = currentPoint(userId) - amount
        if (nextPoint < 0) {
            throw ConcurrencyConflictException(
                failureCode = FailureCode.POINT_BALANCE_INSUFFICIENT,
                message = "포인트 차감 결과가 음수가 될 수 없습니다.",
            )
        }
        pointBalanceByUserId[userId] = nextPoint
    }

    companion object {
        private val DEFAULT_OPERATION_DATE: LocalDate = LocalDate.of(2026, 2, 15)
        private const val DEFAULT_ROULETTE_REWARD_POINTS: Int = 100
    }
}
