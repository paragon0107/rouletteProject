package com.roulette.backend.concurrency

import com.roulette.backend.concurrency.support.ConcurrentExecutionSupport.runConcurrently
import com.roulette.backend.concurrency.support.ConcurrencyConflictException
import com.roulette.backend.concurrency.support.FailureCode
import com.roulette.backend.concurrency.support.InMemoryPointRouletteEngine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PointRouletteConcurrencyScenarioTest {
    @Test
    fun 같은_유저의_동시_룰렛_참여는_1건만_성공한다() {
        val engine = InMemoryPointRouletteEngine(initialDailyBudget = 10_000, rouletteRewardPoints = 100)

        val results = runConcurrently(taskCount = 60) { engine.participate(userId = 1L) }

        results.assertOnlyDomainFailures()
        assertEquals(1, results.successCount())
        assertEquals(59, results.failureCodeCount(FailureCode.ROULETTE_ALREADY_PARTICIPATED_TODAY))
        assertEquals(100, engine.userPointBalance(1L))
        engine.assertInternalConsistency()
    }

    @Test
    fun 여러_유저_동시_참여에서도_일일_예산을_초과하지_않는다() {
        val engine = InMemoryPointRouletteEngine(initialDailyBudget = 1_000, rouletteRewardPoints = 100)

        val results = runConcurrently(taskCount = 50) { index ->
            val userId = (index + 1).toLong()
            engine.participate(userId)
        }

        val snapshot = engine.snapshot()
        results.assertOnlyDomainFailures()
        assertEquals(10, results.successCount())
        assertEquals(40, results.failureCodeCount(FailureCode.BUDGET_INSUFFICIENT))
        assertEquals(1_000, snapshot.usedBudget)
        assertEquals(0, snapshot.remainingBudget)
        engine.assertInternalConsistency()
    }

    @Test
    fun 예산이_충분하면_여러_유저_동시_참여가_모두_성공한다() {
        val engine = InMemoryPointRouletteEngine(initialDailyBudget = 8_000, rouletteRewardPoints = 100)

        val results = runConcurrently(taskCount = 50) { index ->
            val userId = (index + 1).toLong()
            engine.participate(userId)
        }

        assertEquals(50, results.successCount())
        assertEquals(0, results.failureCount())
        assertEquals(5_000, engine.snapshot().usedBudget)
        engine.assertInternalConsistency()
    }

    @Test
    fun 같은_유저의_동시_주문은_보유_포인트_한도만큼만_성공한다() {
        val engine = InMemoryPointRouletteEngine(initialDailyBudget = 100_000, rouletteRewardPoints = 1_000)
        engine.registerProduct(productId = 1L, name = "아메리카노", pricePoints = 300, stock = 100)
        engine.participate(userId = 1L)

        val results = runConcurrently(taskCount = 10) { engine.order(userId = 1L, productId = 1L, quantity = 1) }

        results.assertOnlyDomainFailures()
        assertEquals(3, results.successCount())
        assertEquals(7, results.failureCodeCount(FailureCode.POINT_BALANCE_INSUFFICIENT))
        assertEquals(100, engine.userPointBalance(1L))
        assertEquals(97, engine.productStock(1L))
        engine.assertInternalConsistency()
    }

    @Test
    fun 여러_유저의_동시_주문에서도_재고를_초과하지_않는다() {
        val engine = InMemoryPointRouletteEngine(initialDailyBudget = 100_000, rouletteRewardPoints = 1_000)
        engine.registerProduct(productId = 1L, name = "커피쿠폰", pricePoints = 100, stock = 10)
        seedParticipation(engine, userIdFrom = 1L, userCount = 20)

        val results = runConcurrently(taskCount = 20) { index ->
            val userId = (index + 1).toLong()
            engine.order(userId = userId, productId = 1L, quantity = 1)
        }

        results.assertOnlyDomainFailures()
        assertEquals(10, results.successCount())
        assertEquals(10, results.failureCodeCount(FailureCode.PRODUCT_STOCK_INSUFFICIENT))
        assertEquals(0, engine.productStock(1L))
        engine.assertInternalConsistency()
    }

    @Test
    fun 같은_룰렛_참여를_여러_어드민이_동시에_취소하면_1건만_성공한다() {
        val engine = InMemoryPointRouletteEngine(initialDailyBudget = 100_000, rouletteRewardPoints = 500)
        val participation = engine.participate(userId = 1L)

        val results = runConcurrently(taskCount = 20) {
            engine.cancelParticipationByAdmin(participationId = participation.participationId)
        }

        results.assertOnlyDomainFailures()
        assertEquals(1, results.successCount())
        assertEquals(19, results.failureCodeCount(FailureCode.ROULETTE_PARTICIPATION_ALREADY_CANCELED))
        assertEquals(0, engine.userPointBalance(1L))
        assertEquals(0, engine.snapshot().usedBudget)
        engine.assertInternalConsistency()
    }

    @Test
    fun 같은_주문을_여러_어드민이_동시에_취소하면_1건만_성공한다() {
        val engine = InMemoryPointRouletteEngine(initialDailyBudget = 100_000, rouletteRewardPoints = 1_000)
        engine.registerProduct(productId = 1L, name = "라떼", pricePoints = 300, stock = 10)
        engine.participate(userId = 1L)
        val order = engine.order(userId = 1L, productId = 1L, quantity = 1)

        val results = runConcurrently(taskCount = 20) { engine.cancelOrderByAdmin(orderId = order.orderId) }

        results.assertOnlyDomainFailures()
        assertEquals(1, results.successCount())
        assertEquals(19, results.failureCodeCount(FailureCode.ORDER_ALREADY_CANCELED))
        assertEquals(1_000, engine.userPointBalance(1L))
        assertEquals(10, engine.productStock(1L))
        engine.assertInternalConsistency()
    }

    @Test
    fun 유저_참여와_어드민_예산수정이_경합해도_예산_불변식이_유지된다() {
        val engine = InMemoryPointRouletteEngine(initialDailyBudget = 6_000, rouletteRewardPoints = 100)

        val results = runConcurrently(taskCount = 120) { index ->
            if (index < 80) {
                engine.participate(userId = (index + 1).toLong())
            } else {
                val newBudget = 1_000 + ((index - 80) * 50)
                engine.updateDailyBudgetByAdmin(newBudget)
            }
        }

        val snapshot = engine.snapshot()
        results.assertOnlyDomainFailures()
        assertTrue(snapshot.usedBudget <= snapshot.totalBudget)
        assertTrue(snapshot.usedBudget >= 0)
        assertTrue(snapshot.remainingBudget >= 0)
        engine.assertInternalConsistency()
    }

    @Test
    fun 유저_참여와_어드민_참여취소가_동시에_수행되어도_정합성이_유지된다() {
        val engine = InMemoryPointRouletteEngine(initialDailyBudget = 20_000, rouletteRewardPoints = 100)
        seedParticipation(engine, userIdFrom = 1L, userCount = 30)
        val existingParticipationIds = engine.participationIds()

        val results = runConcurrently(taskCount = 60) { index ->
            if (index < 30) {
                engine.participate(userId = (100 + index).toLong())
            } else {
                val cancelId = existingParticipationIds[index - 30]
                engine.cancelParticipationByAdmin(participationId = cancelId)
            }
        }

        results.assertOnlyDomainFailures()
        assertTrue(results.successCount() >= 30)
        engine.assertInternalConsistency()
    }

    @Test
    fun 유저_주문과_어드민_룰렛취소가_같은_포인트를_두고_경합할때_한쪽만_성공한다() {
        repeat(40) {
            val engine = InMemoryPointRouletteEngine(initialDailyBudget = 10_000, rouletteRewardPoints = 1_000)
            engine.registerProduct(productId = 1L, name = "샌드위치", pricePoints = 1_000, stock = 1)
            val participation = engine.participate(userId = 1L)

            val results = runConcurrently(taskCount = 2) { index ->
                if (index == 0) {
                    engine.order(userId = 1L, productId = 1L, quantity = 1)
                } else {
                    engine.cancelParticipationByAdmin(participationId = participation.participationId)
                }
            }

            results.assertOnlyDomainFailures()
            assertEquals(1, results.successCount())
            assertEquals(1, results.failureCount())
            assertTrue(
                results.failureCodeCount(FailureCode.POINT_ALREADY_USED) == 1 ||
                    results.failureCodeCount(FailureCode.POINT_BALANCE_INSUFFICIENT) == 1,
            )
            engine.assertInternalConsistency()
        }
    }

    @Test
    fun 유저_추가주문과_어드민_이전주문취소가_동시에_발생해도_포인트_재고가_일관된다() {
        val engine = InMemoryPointRouletteEngine(initialDailyBudget = 100_000, rouletteRewardPoints = 1_000)
        engine.registerProduct(productId = 1L, name = "케이크", pricePoints = 500, stock = 2)
        engine.participate(userId = 1L)
        val firstOrder = engine.order(userId = 1L, productId = 1L, quantity = 1)

        val results = runConcurrently(taskCount = 2) { index ->
            if (index == 0) {
                engine.order(userId = 1L, productId = 1L, quantity = 1)
            } else {
                engine.cancelOrderByAdmin(orderId = firstOrder.orderId)
            }
        }

        assertEquals(2, results.successCount())
        assertEquals(0, results.failureCount())
        assertEquals(500, engine.userPointBalance(1L))
        assertEquals(1, engine.productStock(1L))
        engine.assertInternalConsistency()
    }

    @Test
    fun 유저와_어드민이_섞인_대량_요청에서도_전체_불변식이_유지된다() {
        val engine = InMemoryPointRouletteEngine(initialDailyBudget = 30_000, rouletteRewardPoints = 100)
        engine.registerProduct(productId = 1L, name = "에너지바", pricePoints = 100, stock = 300)
        seedParticipation(engine, userIdFrom = 1L, userCount = 40)

        val results = runConcurrently(taskCount = 400) { index ->
            when (index % 5) {
                0 -> engine.participate(userId = (index % 120 + 1).toLong())
                1 -> engine.order(userId = (index % 80 + 1).toLong(), productId = 1L, quantity = 1)
                2 -> engine.cancelParticipationByAdmin(participationId = engine.firstParticipationIdOrNull() ?: -1L)
                3 -> engine.cancelOrderByAdmin(orderId = engine.firstPlacedOrderIdOrNull() ?: -1L)
                else -> engine.updateDailyBudgetByAdmin(5_000 + ((index % 40) * 100))
            }
        }

        results.assertOnlyDomainFailures()
        val snapshot = engine.snapshot()
        assertTrue(snapshot.usedBudget <= snapshot.totalBudget)
        assertTrue(snapshot.usedBudget >= 0)
        assertTrue(snapshot.remainingBudget >= 0)
        engine.assertInternalConsistency()
    }

    @Test
    fun 유저_다중_주문과_어드민_다중_취소를_동시에_반복해도_중복_취소가_발생하지_않는다() {
        val engine = InMemoryPointRouletteEngine(initialDailyBudget = 100_000, rouletteRewardPoints = 1_000)
        engine.registerProduct(productId = 1L, name = "디저트", pricePoints = 100, stock = 300)
        seedParticipation(engine, userIdFrom = 1L, userCount = 50)

        runConcurrently(taskCount = 100) { index ->
            val userId = (index % 50 + 1).toLong()
            engine.order(userId = userId, productId = 1L, quantity = 1)
        }

        val orderIds = engine.placedOrderIds()
        val results = runConcurrently(taskCount = orderIds.size * 2) { index ->
            val orderId = orderIds[index % orderIds.size]
            engine.cancelOrderByAdmin(orderId = orderId)
        }

        results.assertOnlyDomainFailures()
        assertEquals(orderIds.size, results.successCount())
        assertEquals(orderIds.size, results.failureCodeCount(FailureCode.ORDER_ALREADY_CANCELED))
        engine.assertInternalConsistency()
    }

    private fun seedParticipation(
        engine: InMemoryPointRouletteEngine,
        userIdFrom: Long,
        userCount: Int,
    ) {
        repeat(userCount) { index ->
            val userId = userIdFrom + index
            engine.participate(userId = userId)
        }
    }

    private fun List<Result<*>>.successCount(): Int {
        return count { it.isSuccess }
    }

    private fun List<Result<*>>.failureCount(): Int {
        return count { it.isFailure }
    }

    private fun List<Result<*>>.failureCodeCount(code: FailureCode): Int {
        return failureCodes().count { it == code }
    }

    private fun List<Result<*>>.failureCodes(): List<FailureCode> {
        return mapNotNull { result ->
            val exception = result.exceptionOrNull()
            if (exception is ConcurrencyConflictException) exception.failureCode else null
        }
    }

    private fun List<Result<*>>.assertOnlyDomainFailures() {
        val unexpected = mapNotNull { it.exceptionOrNull() }
            .filter { exception -> exception !is ConcurrencyConflictException }
        assertTrue(unexpected.isEmpty(), "예상하지 못한 예외가 발생했습니다: $unexpected")
    }
}
