package com.roulette.backend.domain.budget.domain

import com.roulette.backend.common.entity.BaseTimeEntity
import com.roulette.backend.common.util.BusinessConstants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.persistence.Version
import java.time.LocalDate

@Entity
@Table(
    name = "daily_budgets",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_daily_budgets_budget_date", columnNames = ["budget_date"]),
    ],
)
class DailyBudget(
    budgetDate: LocalDate,
    totalBudgetPoints: Int = BusinessConstants.DEFAULT_DAILY_BUDGET_POINTS,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "budget_date", nullable = false)
    var budgetDate: LocalDate = budgetDate
        protected set

    @Column(name = "total_budget_points", nullable = false)
    var totalBudgetPoints: Int = totalBudgetPoints
        protected set

    @Column(name = "used_budget_points", nullable = false)
    var usedBudgetPoints: Int = 0
        protected set

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        protected set

    init {
        validateBudgetValue(totalBudgetPoints)
    }

    fun remainingBudgetPoints(): Int {
        return totalBudgetPoints - usedBudgetPoints
    }

    fun canAllocate(points: Int): Boolean {
        if (points <= 0) return false
        return usedBudgetPoints + points <= totalBudgetPoints
    }

    fun allocate(points: Int) {
        validatePositivePoint(points)
        require(canAllocate(points)) { "일일 예산이 부족합니다." }
        usedBudgetPoints += points
    }

    fun release(points: Int) {
        validatePositivePoint(points)
        require(usedBudgetPoints >= points) { "회수 포인트가 사용 예산보다 클 수 없습니다." }
        usedBudgetPoints -= points
    }

    fun updateTotalBudget(nextTotalBudgetPoints: Int) {
        validateBudgetValue(nextTotalBudgetPoints)
        require(nextTotalBudgetPoints >= usedBudgetPoints) {
            "총 예산은 현재 사용 예산보다 작을 수 없습니다."
        }
        totalBudgetPoints = nextTotalBudgetPoints
    }

    private fun validatePositivePoint(points: Int) {
        require(points > 0) { "포인트는 1 이상이어야 합니다." }
    }

    private fun validateBudgetValue(points: Int) {
        require(points >= 0) { "예산은 0 이상이어야 합니다." }
    }
}
