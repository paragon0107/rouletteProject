package com.roulette.backend.domain.product.domain

import com.roulette.backend.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.persistence.Version

@Entity
@Table(
    name = "products",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_products_name", columnNames = ["name"]),
    ],
)
class Product(
    name: String,
    description: String,
    pricePoints: Int,
    stock: Int,
    status: ProductStatus = ProductStatus.ACTIVE,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "name", nullable = false, length = MAX_NAME_LENGTH)
    var name: String = name.trim()
        protected set

    @Column(name = "description", nullable = false, length = MAX_DESCRIPTION_LENGTH)
    var description: String = description.trim()
        protected set

    @Column(name = "price_points", nullable = false)
    var pricePoints: Int = pricePoints
        protected set

    @Column(name = "stock", nullable = false)
    var stock: Int = stock
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: ProductStatus = status
        protected set

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        protected set

    init {
        validateName(this.name)
        validateDescription(this.description)
        validatePrice(pricePoints)
        validateStock(stock)
    }

    fun updateInfo(
        nextName: String,
        nextDescription: String,
        nextPricePoints: Int,
        nextStock: Int,
        nextStatus: ProductStatus,
    ) {
        validateName(nextName)
        validateDescription(nextDescription)
        validatePrice(nextPricePoints)
        validateStock(nextStock)
        name = nextName.trim()
        description = nextDescription.trim()
        pricePoints = nextPricePoints
        stock = nextStock
        status = nextStatus
    }

    fun activate() {
        status = ProductStatus.ACTIVE
    }

    fun deactivate() {
        status = ProductStatus.INACTIVE
    }

    fun decreaseStock(quantity: Int) {
        require(quantity > 0) { "차감 수량은 1 이상이어야 합니다." }
        require(status == ProductStatus.ACTIVE) { "비활성 상품은 주문할 수 없습니다." }
        require(stock >= quantity) { "재고가 부족합니다." }
        stock -= quantity
    }

    fun increaseStock(quantity: Int) {
        require(quantity > 0) { "증가 수량은 1 이상이어야 합니다." }
        stock += quantity
    }

    private fun validateName(candidate: String) {
        val trimmedCandidate = candidate.trim()
        require(trimmedCandidate.length in MIN_NAME_LENGTH..MAX_NAME_LENGTH) {
            "상품명은 $MIN_NAME_LENGTH~${MAX_NAME_LENGTH}자여야 합니다."
        }
    }

    private fun validateDescription(candidate: String) {
        val trimmedCandidate = candidate.trim()
        require(trimmedCandidate.length in MIN_DESCRIPTION_LENGTH..MAX_DESCRIPTION_LENGTH) {
            "상품 설명은 $MIN_DESCRIPTION_LENGTH~${MAX_DESCRIPTION_LENGTH}자여야 합니다."
        }
    }

    private fun validatePrice(points: Int) {
        require(points in MIN_PRICE_POINTS..MAX_PRICE_POINTS) {
            "상품 가격은 $MIN_PRICE_POINTS~$MAX_PRICE_POINTS 사이여야 합니다."
        }
    }

    private fun validateStock(quantity: Int) {
        require(quantity in MIN_STOCK..MAX_STOCK) {
            "재고는 $MIN_STOCK~$MAX_STOCK 사이여야 합니다."
        }
    }

    companion object {
        const val MIN_NAME_LENGTH: Int = 2
        const val MAX_NAME_LENGTH: Int = 100
        const val MIN_DESCRIPTION_LENGTH: Int = 1
        const val MAX_DESCRIPTION_LENGTH: Int = 500
        const val MIN_PRICE_POINTS: Int = 1
        const val MAX_PRICE_POINTS: Int = 1_000_000
        const val MIN_STOCK: Int = 0
        const val MAX_STOCK: Int = 1_000_000
    }
}
