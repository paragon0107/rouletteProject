package com.roulette.backend.domain.user.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class MockLoginRequest(
    @field:NotBlank(message = "닉네임은 비어 있을 수 없습니다.")
    @field:Size(min = 2, max = 30, message = "닉네임은 2~30자여야 합니다.")
    @field:Pattern(
        regexp = "^[a-zA-Z0-9가-힣_-]+$",
        message = "닉네임은 영문/숫자/한글/_/- 만 사용할 수 있습니다.",
    )
    val nickname: String,
)
