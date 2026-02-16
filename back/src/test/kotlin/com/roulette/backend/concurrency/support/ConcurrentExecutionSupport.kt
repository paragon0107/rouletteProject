package com.roulette.backend.concurrency.support

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object ConcurrentExecutionSupport {
    fun <T> runConcurrently(
        taskCount: Int,
        timeoutSeconds: Long = 10,
        action: (Int) -> T,
    ): List<Result<T>> {
        require(taskCount > 0) { "taskCount는 1 이상이어야 합니다." }
        // 모든 작업이 startLatch 대기 지점까지 도달해야 하므로 taskCount와 동일한 풀 크기를 사용한다.
        val executor = Executors.newFixedThreadPool(taskCount)
        val readyLatch = CountDownLatch(taskCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(taskCount)
        val resultByIndex = ConcurrentHashMap<Int, Result<T>>()

        repeat(taskCount) { index ->
            executor.submit {
                readyLatch.countDown()
                startLatch.await()
                resultByIndex[index] = runCatching { action(index) }
                doneLatch.countDown()
            }
        }

        if (!readyLatch.await(timeoutSeconds, TimeUnit.SECONDS)) {
            executor.shutdownNow()
            throw IllegalStateException("동시 실행 준비 시간이 초과되었습니다.")
        }

        startLatch.countDown()
        if (!doneLatch.await(timeoutSeconds, TimeUnit.SECONDS)) {
            executor.shutdownNow()
            throw IllegalStateException("동시 실행 완료 시간이 초과되었습니다.")
        }

        executor.shutdownNow()
        return (0 until taskCount).map { index ->
            resultByIndex[index]
                ?: Result.failure(IllegalStateException("결과가 누락되었습니다. index=$index"))
        }
    }
}
