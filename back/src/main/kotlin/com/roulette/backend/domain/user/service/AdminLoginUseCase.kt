package com.roulette.backend.domain.user.service

import com.roulette.backend.common.exception.BusinessException
import com.roulette.backend.common.util.AdminAuthConstants
import org.springframework.stereotype.Service

@Service
class AdminLoginUseCase {
    fun execute(adminCode: String): AdminLoginResult {
        val trimmedCode = adminCode.trim()
        if (trimmedCode == AdminAuthConstants.ADMIN_LOGIN_CODE) {
            return AdminLoginResult(
                headerName = AdminAuthConstants.HEADER_ADMIN_TOKEN,
                adminToken = AdminAuthConstants.ADMIN_ACCESS_TOKEN,
            )
        }
        throw BusinessException("AUTH_UNAUTHORIZED", "어드민 인증 코드가 올바르지 않습니다.")
    }
}

data class AdminLoginResult(
    val headerName: String,
    val adminToken: String,
)
