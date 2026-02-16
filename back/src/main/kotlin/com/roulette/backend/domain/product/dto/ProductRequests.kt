package com.roulette.backend.domain.product.dto

import com.roulette.backend.domain.product.domain.ProductStatus
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateProductRequest(
    @field:NotBlank(message = "상품명은 비어 있을 수 없습니다.")
    @field:Size(min = 2, max = 100, message = "상품명은 2~100자여야 합니다.")
    val name: String,
    @field:NotBlank(message = "상품 설명은 비어 있을 수 없습니다.")
    @field:Size(min = 1, max = 500, message = "상품 설명은 1~500자여야 합니다.")
    val description: String,
    @field:Min(value = 1, message = "상품 가격은 1 이상이어야 합니다.")
    @field:Max(value = 1_000_000, message = "상품 가격은 1,000,000 이하여야 합니다.")
    val pricePoints: Int,
    @field:Min(value = 0, message = "재고는 0 이상이어야 합니다.")
    @field:Max(value = 1_000_000, message = "재고는 1,000,000 이하여야 합니다.")
    val stock: Int,
    val status: ProductStatus,
)

data class UpdateProductRequest(
    @field:Size(min = 2, max = 100, message = "상품명은 2~100자여야 합니다.")
    val name: String? = null,
    @field:Size(min = 1, max = 500, message = "상품 설명은 1~500자여야 합니다.")
    val description: String? = null,
    @field:Min(value = 1, message = "상품 가격은 1 이상이어야 합니다.")
    @field:Max(value = 1_000_000, message = "상품 가격은 1,000,000 이하여야 합니다.")
    val pricePoints: Int? = null,
    @field:Min(value = 0, message = "재고는 0 이상이어야 합니다.")
    @field:Max(value = 1_000_000, message = "재고는 1,000,000 이하여야 합니다.")
    val stock: Int? = null,
    val status: ProductStatus? = null,
)
