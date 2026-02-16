package com.roulette.backend.domain.user.controller

import com.roulette.backend.common.response.ApiResponse
import com.roulette.backend.domain.user.dto.AdminLoginRequest
import com.roulette.backend.domain.user.dto.AdminLoginResponse
import com.roulette.backend.domain.user.dto.MockLoginRequest
import com.roulette.backend.domain.user.dto.MockLoginResponse
import com.roulette.backend.domain.user.dto.toResponse
import com.roulette.backend.domain.user.service.AdminLoginUseCase
import com.roulette.backend.domain.user.service.MockLoginUseCase
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val mockLoginUseCase: MockLoginUseCase,
    private val adminLoginUseCase: AdminLoginUseCase,
) {
    @PostMapping("/login")
    fun mockLogin(
        @Valid @RequestBody request: MockLoginRequest,
    ): ApiResponse<MockLoginResponse> {
        val result = mockLoginUseCase.execute(request.nickname)
        return ApiResponse(result.toResponse())
    }

    @PostMapping("/admin-login")
    fun adminLogin(
        @Valid @RequestBody request: AdminLoginRequest,
    ): ApiResponse<AdminLoginResponse> {
        val result = adminLoginUseCase.execute(request.adminCode)
        return ApiResponse(result.toResponse())
    }
}
