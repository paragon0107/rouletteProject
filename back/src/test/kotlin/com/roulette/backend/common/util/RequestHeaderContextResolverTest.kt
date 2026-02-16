package com.roulette.backend.common.util

import com.roulette.backend.common.exception.BusinessException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest

class RequestHeaderContextResolverTest {
    private val resolver = RequestHeaderContextResolver()

    @Test
    fun 어드민_토큰이_올바르면_관리자_검증에_성공한다() {
        val request = MockHttpServletRequest()
        request.addHeader(AdminAuthConstants.HEADER_ADMIN_TOKEN, AdminAuthConstants.ADMIN_ACCESS_TOKEN)

        assertDoesNotThrow { resolver.requireAdmin(request) }
    }

    @Test
    fun 어드민_토큰이_없으면_인증_예외가_발생한다() {
        val request = MockHttpServletRequest()

        val exception = assertThrows(BusinessException::class.java) {
            resolver.requireAdmin(request)
        }

        assertEquals("AUTH_UNAUTHORIZED", exception.code)
    }

    @Test
    fun 어드민_토큰이_유효하지_않으면_권한_예외가_발생한다() {
        val request = MockHttpServletRequest()
        request.addHeader(AdminAuthConstants.HEADER_ADMIN_TOKEN, "INVALID-TOKEN")

        val exception = assertThrows(BusinessException::class.java) {
            resolver.requireAdmin(request)
        }

        assertEquals("AUTH_FORBIDDEN", exception.code)
    }
}
