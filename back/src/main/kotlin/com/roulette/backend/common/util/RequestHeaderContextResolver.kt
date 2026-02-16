package com.roulette.backend.common.util

import com.roulette.backend.common.exception.BusinessException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

@Component
class RequestHeaderContextResolver {
    fun resolveUserId(request: HttpServletRequest): Long {
        val rawUserId = request.getHeader(HEADER_USER_ID)
            ?: throw BusinessException("AUTH_UNAUTHORIZED", "X-USER-ID 헤더가 필요합니다.")
        val userId = rawUserId.toLongOrNull()
            ?: throw BusinessException("AUTH_UNAUTHORIZED", "X-USER-ID 형식이 올바르지 않습니다.")
        if (userId > 0) return userId
        throw BusinessException("AUTH_UNAUTHORIZED", "X-USER-ID는 1 이상이어야 합니다.")
    }

    fun requireAdmin(request: HttpServletRequest) {
        val adminToken = request.getHeader(AdminAuthConstants.HEADER_ADMIN_TOKEN)?.trim()
            ?: throw BusinessException("AUTH_UNAUTHORIZED", "X-ADMIN-TOKEN 헤더가 필요합니다.")
        if (adminToken == AdminAuthConstants.ADMIN_ACCESS_TOKEN) return
        throw BusinessException("AUTH_FORBIDDEN", "관리자 인증 토큰이 유효하지 않습니다.")
    }

    companion object {
        private const val HEADER_USER_ID = "X-USER-ID"
    }
}
