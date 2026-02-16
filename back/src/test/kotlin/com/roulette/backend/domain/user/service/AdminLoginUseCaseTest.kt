package com.roulette.backend.domain.user.service

import com.roulette.backend.common.exception.BusinessException
import com.roulette.backend.common.util.AdminAuthConstants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AdminLoginUseCaseTest {
    private val useCase = AdminLoginUseCase()

    @Test
    fun 올바른_인증코드를_입력하면_고정_어드민_토큰을_반환한다() {
        val result = useCase.execute(AdminAuthConstants.ADMIN_LOGIN_CODE)

        assertEquals(AdminAuthConstants.HEADER_ADMIN_TOKEN, result.headerName)
        assertEquals(AdminAuthConstants.ADMIN_ACCESS_TOKEN, result.adminToken)
    }

    @Test
    fun 올바르지_않은_인증코드를_입력하면_인증_예외가_발생한다() {
        val exception = assertThrows(BusinessException::class.java) {
            useCase.execute("WRONG-CODE")
        }

        assertEquals("AUTH_UNAUTHORIZED", exception.code)
    }
}
