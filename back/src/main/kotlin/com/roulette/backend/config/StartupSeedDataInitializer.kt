package com.roulette.backend.config

import com.roulette.backend.common.exception.BusinessException
import com.roulette.backend.common.util.BusinessConstants
import com.roulette.backend.domain.budget.repository.DailyBudgetReadRepository
import com.roulette.backend.domain.budget.repository.DailyBudgetWriteRepository
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
import com.roulette.backend.domain.roulette.repository.RouletteParticipationWriteRepository
import com.roulette.backend.domain.user.domain.UserRole
import com.roulette.backend.domain.user.repository.UserReadRepository
import com.roulette.backend.domain.user.repository.UserWriteRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

@Component
class StartupSeedDataInitializer(
    private val userReadRepository: UserReadRepository,
    private val userWriteRepository: UserWriteRepository,
    private val dailyBudgetReadRepository: DailyBudgetReadRepository,
    private val dailyBudgetWriteRepository: DailyBudgetWriteRepository,
    private val rouletteParticipationWriteRepository: RouletteParticipationWriteRepository,
    private val pointUnitReadRepository: PointUnitReadRepository,
    private val pointUnitWriteRepository: PointUnitWriteRepository,
    private val pointTransactionWriteRepository: PointTransactionWriteRepository,
    private val productReadRepository: ProductReadRepository,
    private val productWriteRepository: ProductWriteRepository,
    private val orderWriteRepository: OrderWriteRepository,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        seedDataIfNeeded()
    }

    @Transactional
    fun seedDataIfNeeded() {
        if (alreadySeeded()) {
            log.info("기본 시드 데이터가 이미 존재하여 초기화를 건너뜁니다.")
            return
        }

        val seedUsers = createSeedUsers()
        val seedProducts = createSeedProducts()
        val today = LocalDate.now(ZoneOffset.UTC)

        seedUserBTransactions(seedUsers.userBId, seedProducts, today)
        seedUserCTransactions(seedUsers.userCId, seedProducts, today)
        seedUserDTransactions(seedUsers.userDId, seedProducts, today)

        pointUnitWriteRepository.expirePointUnits(LocalDateTime.now(ZoneOffset.UTC))
        log.info(
            "기본 시드 데이터 초기화 완료: userA={}, userB={}, userC={}, userD={}",
            seedUsers.userAId,
            seedUsers.userBId,
            seedUsers.userCId,
            seedUsers.userDId,
        )
    }

    private fun alreadySeeded(): Boolean {
        return userReadRepository.existsByNickname(USER_A_NICKNAME)
    }

    private fun createSeedUsers(): SeedUsers {
        val userAId = createOrGetUser(USER_A_NICKNAME)
        val userBId = createOrGetUser(USER_B_NICKNAME)
        val userCId = createOrGetUser(USER_C_NICKNAME)
        val userDId = createOrGetUser(USER_D_NICKNAME)
        return SeedUsers(
            userAId = userAId,
            userBId = userBId,
            userCId = userCId,
            userDId = userDId,
        )
    }

    private fun createOrGetUser(nickname: String): Long {
        val existingUser = userReadRepository.findByNickname(nickname)
        if (existingUser != null) return requireNotNull(existingUser.id)
        return try {
            userWriteRepository.insertUser(nickname, UserRole.USER)
        } catch (_: DataIntegrityViolationException) {
            val duplicatedUser = userReadRepository.findByNickname(nickname)
                ?: throw BusinessException("SEED_USER_CONFLICT", "사용자 시드 생성 중 충돌이 발생했습니다.")
            requireNotNull(duplicatedUser.id)
        }
    }

    private fun createSeedProducts(): SeedProducts {
        val miniCoffee = createOrGetProduct(
            name = "시드_미니커피",
            description = "시드 데이터용 저가 상품",
            pricePoints = 40,
            stock = 20_000,
        )
        val coffee = createOrGetProduct(
            name = "시드_아메리카노",
            description = "시드 데이터용 기본 상품",
            pricePoints = 120,
            stock = 15_000,
        )
        val snack = createOrGetProduct(
            name = "시드_스낵박스",
            description = "시드 데이터용 중가 상품",
            pricePoints = 350,
            stock = 10_000,
        )
        val premium = createOrGetProduct(
            name = "시드_프리미엄박스",
            description = "시드 데이터용 고가 상품",
            pricePoints = 900,
            stock = 5_000,
        )
        return SeedProducts(
            miniCoffee = miniCoffee,
            coffee = coffee,
            snack = snack,
            premium = premium,
        )
    }

    private fun createOrGetProduct(
        name: String,
        description: String,
        pricePoints: Int,
        stock: Int,
    ): SeedProduct {
        val existingProduct = productReadRepository.findByName(name)
        if (existingProduct != null) {
            return SeedProduct(
                productId = requireNotNull(existingProduct.id),
                pricePoints = existingProduct.pricePoints,
            )
        }
        val createdProductId = productWriteRepository.insertProduct(
            name = name,
            description = description,
            pricePoints = pricePoints,
            stock = stock,
            status = ProductStatus.ACTIVE,
        )
        return SeedProduct(productId = createdProductId, pricePoints = pricePoints)
    }

    private fun seedUserBTransactions(
        userId: Long,
        seedProducts: SeedProducts,
        today: LocalDate,
    ) {
        val rewards = listOf(100, 300, 500, 300, 1000)
        rewards.forEachIndexed { index, rewardPoints ->
            val participationDate = today.minusDays((5 - index).toLong())
            createParticipationWithReward(
                userId = userId,
                participationDate = participationDate,
                rewardPoints = rewardPoints,
                sequence = index,
            )
        }
        repeat(12) { index ->
            val orderedAt = today.minusDays((index % 4).toLong())
                .atTime(10 + (index % 5), (index * 7) % 60)
            val product = pickSeedProduct(index, seedProducts)
            val quantity = if (index % 6 == 0) 2 else 1
            val shouldCancel = index % 5 == 0
            createOrderWithOptionalCancel(
                userId = userId,
                product = product,
                quantity = quantity,
                orderedAt = orderedAt,
                shouldCancel = shouldCancel,
            )
        }
    }

    private fun seedUserCTransactions(
        userId: Long,
        seedProducts: SeedProducts,
        today: LocalDate,
    ) {
        repeat(80) { index ->
            val participationDate = today.minusDays((80 - index).toLong())
            val rewardPoints = rewardPointsByIndex(index)
            createParticipationWithReward(
                userId = userId,
                participationDate = participationDate,
                rewardPoints = rewardPoints,
                sequence = index,
            )
        }
        repeat(140) { index ->
            val orderedAt = today.minusDays((index % 24).toLong())
                .atTime(9 + (index % 12), (index * 11) % 60)
            val product = pickSeedProduct(index + 3, seedProducts)
            val quantity = if (index % 19 == 0) 2 else 1
            val shouldCancel = index % 4 == 0
            createOrderWithOptionalCancel(
                userId = userId,
                product = product,
                quantity = quantity,
                orderedAt = orderedAt,
                shouldCancel = shouldCancel,
            )
        }
    }

    private fun seedUserDTransactions(
        userId: Long,
        seedProducts: SeedProducts,
        today: LocalDate,
    ) {
        repeat(90) { index ->
            val participationDate = today.minusDays((90 - index).toLong())
            val rewardPoints = rewardPointsByIndex(index + 1)
            createParticipationWithReward(
                userId = userId,
                participationDate = participationDate,
                rewardPoints = rewardPoints,
                sequence = index,
            )
        }
        repeat(180) { index ->
            val orderedAt = today.minusDays((index % 28).toLong())
                .atTime(8 + (index % 13), (index * 13) % 60)
            val product = pickSeedProduct(index + 7, seedProducts)
            val quantity = if (index % 23 == 0) 2 else 1
            val shouldCancel = index % 3 == 0
            createOrderWithOptionalCancel(
                userId = userId,
                product = product,
                quantity = quantity,
                orderedAt = orderedAt,
                shouldCancel = shouldCancel,
            )
        }
    }

    private fun createParticipationWithReward(
        userId: Long,
        participationDate: LocalDate,
        rewardPoints: Int,
        sequence: Int,
    ) {
        ensureDailyBudgetExists(participationDate)
        allocateBudgetOrThrow(participationDate, rewardPoints)
        val awardedAt = participationDate.atTime(9, 0).plusMinutes((sequence % 60).toLong())
        val pointExpiresAt = awardedAt.plusDays(BusinessConstants.POINT_EXPIRATION_DAYS)
        val participationId = rouletteParticipationWriteRepository.insertParticipation(
            userId = userId,
            participationDate = participationDate,
            awardedPoints = rewardPoints,
            awardedAt = awardedAt,
            pointExpiresAt = pointExpiresAt,
        )
        pointUnitWriteRepository.insertPointUnit(
            userId = userId,
            eventType = PointEventType.ROULETTE_REWARD,
            amount = rewardPoints,
            earnedAt = awardedAt,
            expiresAt = pointExpiresAt,
            sourceParticipationId = participationId,
        )
        pointTransactionWriteRepository.insertTransaction(
            userId = userId,
            eventType = PointEventType.ROULETTE_REWARD,
            direction = PointTransactionDirection.CREDIT,
            amount = rewardPoints,
            occurredAt = awardedAt,
            participationId = participationId,
        )
    }

    private fun ensureDailyBudgetExists(participationDate: LocalDate) {
        val existingBudget = dailyBudgetReadRepository.findByBudgetDate(participationDate)
        if (existingBudget != null) return
        try {
            dailyBudgetWriteRepository.insertDailyBudget(
                budgetDate = participationDate,
                totalBudgetPoints = BusinessConstants.DEFAULT_DAILY_BUDGET_POINTS,
            )
        } catch (_: DataIntegrityViolationException) {
            // 동시 생성 경합으로 이미 생성된 경우 그대로 사용한다.
        }
    }

    private fun allocateBudgetOrThrow(
        participationDate: LocalDate,
        rewardPoints: Int,
    ) {
        val updatedRows = dailyBudgetWriteRepository.allocateBudgetIfPossible(participationDate, rewardPoints)
        if (updatedRows > 0) return
        throw BusinessException("SEED_BUDGET_INSUFFICIENT", "시드 데이터 예산 할당에 실패했습니다.")
    }

    private fun createOrderWithOptionalCancel(
        userId: Long,
        product: SeedProduct,
        quantity: Int,
        orderedAt: LocalDateTime,
        shouldCancel: Boolean,
    ) {
        val requiredPoints = product.pricePoints * quantity
        val availableBalance = pointUnitReadRepository.sumAvailableBalance(userId, orderedAt).toInt()
        if (availableBalance < requiredPoints) return

        val decreasedRows = productWriteRepository.decreaseStockIfPossible(product.productId, quantity)
        if (decreasedRows == 0) return

        val orderId = orderWriteRepository.insertOrder(
            userId = userId,
            productId = product.productId,
            quantity = quantity,
            usedPoints = requiredPoints,
            orderedAt = orderedAt,
        )
        deductPointsFromOldestUnits(userId, requiredPoints, orderedAt, orderId)
        if (!shouldCancel) return
        cancelOrderAndRefund(
            userId = userId,
            orderId = orderId,
            productId = product.productId,
            quantity = quantity,
            refundedPoints = requiredPoints,
            canceledAt = orderedAt.plusMinutes(20),
        )
    }

    private fun deductPointsFromOldestUnits(
        userId: Long,
        requiredPoints: Int,
        orderedAt: LocalDateTime,
        orderId: Long,
    ) {
        var remainingPoints = requiredPoints
        val availablePointUnits = pointUnitReadRepository
            .findAllByUserIdAndStatusOrderByExpiresAtAsc(userId, PointUnitStatus.AVAILABLE)
            .filter { pointUnit -> pointUnit.expiresAt > orderedAt }
        availablePointUnits.forEach { pointUnit ->
            if (remainingPoints == 0) return@forEach
            val pointUnitId = requireNotNull(pointUnit.id)
            val deductAmount = minOf(pointUnit.remainingAmount, remainingPoints)
            if (deductAmount == 0) return@forEach
            val updatedRows = pointUnitWriteRepository.deductAmountIfPossible(pointUnitId, deductAmount)
            if (updatedRows == 0) {
                throw BusinessException("SEED_POINT_DEDUCTION_FAILED", "시드 포인트 차감에 실패했습니다.")
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
        throw BusinessException("SEED_POINT_DEDUCTION_FAILED", "시드 포인트 잔액 정합성에 실패했습니다.")
    }

    private fun cancelOrderAndRefund(
        userId: Long,
        orderId: Long,
        productId: Long,
        quantity: Int,
        refundedPoints: Int,
        canceledAt: LocalDateTime,
    ) {
        val canceledRows = orderWriteRepository.cancelOrder(orderId)
        if (canceledRows == 0) return
        productWriteRepository.increaseStock(productId, quantity)
        val expiresAt = canceledAt.plusDays(BusinessConstants.POINT_EXPIRATION_DAYS)
        pointUnitWriteRepository.insertPointUnit(
            userId = userId,
            eventType = PointEventType.ORDER_REFUND,
            amount = refundedPoints,
            earnedAt = canceledAt,
            expiresAt = expiresAt,
            sourceOrderId = orderId,
        )
        pointTransactionWriteRepository.insertTransaction(
            userId = userId,
            eventType = PointEventType.ORDER_REFUND,
            direction = PointTransactionDirection.CREDIT,
            amount = refundedPoints,
            occurredAt = canceledAt,
            orderId = orderId,
        )
    }

    private fun rewardPointsByIndex(index: Int): Int {
        return when (index % 10) {
            0, 1, 2, 3 -> 100
            4, 5, 6 -> 300
            7, 8 -> 500
            else -> 1000
        }
    }

    private fun pickSeedProduct(
        index: Int,
        seedProducts: SeedProducts,
    ): SeedProduct {
        return when (index % 10) {
            0 -> seedProducts.premium
            1 -> seedProducts.snack
            2, 3 -> seedProducts.coffee
            else -> seedProducts.miniCoffee
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(StartupSeedDataInitializer::class.java)
        private const val USER_A_NICKNAME = "userA"
        private const val USER_B_NICKNAME = "userB"
        private const val USER_C_NICKNAME = "userC"
        private const val USER_D_NICKNAME = "userD"
    }
}

private data class SeedUsers(
    val userAId: Long,
    val userBId: Long,
    val userCId: Long,
    val userDId: Long,
)

private data class SeedProducts(
    val miniCoffee: SeedProduct,
    val coffee: SeedProduct,
    val snack: SeedProduct,
    val premium: SeedProduct,
)

private data class SeedProduct(
    val productId: Long,
    val pricePoints: Int,
)
