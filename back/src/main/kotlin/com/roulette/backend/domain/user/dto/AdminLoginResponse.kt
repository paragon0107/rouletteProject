package com.roulette.backend.domain.user.dto

import com.roulette.backend.domain.user.service.AdminLoginResult

data class AdminLoginResponse(
    val headerName: String,
    val adminToken: String,
)

fun AdminLoginResult.toResponse(): AdminLoginResponse {
    return AdminLoginResponse(
        headerName = headerName,
        adminToken = adminToken,
    )
}
