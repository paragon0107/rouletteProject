package com.roulette.backend.domain.user.dto

import com.roulette.backend.domain.user.domain.UserRole
import com.roulette.backend.domain.user.service.MockLoginResult

data class MockLoginResponse(
    val userId: Long,
    val nickname: String,
    val role: UserRole,
)

fun MockLoginResult.toResponse(): MockLoginResponse {
    return MockLoginResponse(
        userId = userId,
        nickname = nickname,
        role = role,
    )
}
