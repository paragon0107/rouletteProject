package com.roulette.backend.domain.budget.repository

import com.roulette.backend.domain.budget.domain.DailyBudget
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface DailyBudgetReadRepository : JpaRepository<DailyBudget, Long> {
    fun findByBudgetDate(budgetDate: LocalDate): DailyBudget?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM DailyBudget b WHERE b.budgetDate = :budgetDate")
    fun findByBudgetDateForUpdate(
        @Param("budgetDate") budgetDate: LocalDate,
    ): DailyBudget?
}
