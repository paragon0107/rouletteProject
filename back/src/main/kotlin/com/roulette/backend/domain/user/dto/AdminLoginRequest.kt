package com.roulette.backend.domain.user.dto

import jakarta.validation.constraints.NotBlank

data class AdminLoginRequest(
    @field:NotBlank(message = "어드민 인증 코드는 비어 있을 수 없습니다.")
    val adminCode: String,
)
