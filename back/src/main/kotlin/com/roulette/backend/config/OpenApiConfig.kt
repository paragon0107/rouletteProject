package com.roulette.backend.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.parameters.Parameter
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.HandlerMethod

@Configuration
class OpenApiConfig {
    @Bean
    fun openApi(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Point Roulette API")
                    .version("v1")
                    .description("Point Roulette backend API"),
            )
    }

    @Bean
    fun headerOperationCustomizer(): OperationCustomizer {
        return OperationCustomizer { operation, handlerMethod ->
            when {
                isAuthController(handlerMethod) -> operation
                isAdminController(handlerMethod) -> applyAdminHeader(operation)
                else -> applyUserHeader(operation)
            }
        }
    }

    private fun isAuthController(handlerMethod: HandlerMethod): Boolean {
        return handlerMethod.beanType.simpleName == AUTH_CONTROLLER_NAME
    }

    private fun isAdminController(handlerMethod: HandlerMethod): Boolean {
        return handlerMethod.beanType.simpleName.startsWith(ADMIN_CONTROLLER_PREFIX)
    }

    private fun applyUserHeader(operation: io.swagger.v3.oas.models.Operation): io.swagger.v3.oas.models.Operation {
        addUserIdHeader(operation)
        return operation
    }

    private fun applyAdminHeader(operation: io.swagger.v3.oas.models.Operation): io.swagger.v3.oas.models.Operation {
        addAdminTokenHeader(operation)
        return operation
    }

    private fun addUserIdHeader(operation: io.swagger.v3.oas.models.Operation) {
        if (hasHeader(operation, USER_ID_HEADER_NAME)) return
        operation.addParametersItem(
            Parameter()
                .name(USER_ID_HEADER_NAME)
                .`in`("header")
                .required(true)
                .description("사용자 식별자(1 이상의 정수)")
                .schema(IntegerSchema().format("int64").minimum(1.toBigDecimal())),
        )
    }

    private fun addAdminTokenHeader(operation: io.swagger.v3.oas.models.Operation) {
        if (hasHeader(operation, ADMIN_TOKEN_HEADER_NAME)) return
        operation.addParametersItem(
            Parameter()
                .name(ADMIN_TOKEN_HEADER_NAME)
                .`in`("header")
                .required(true)
                .description("관리자 인증 토큰")
                .schema(StringSchema()),
        )
    }

    private fun hasHeader(
        operation: io.swagger.v3.oas.models.Operation,
        headerName: String,
    ): Boolean {
        val parameters = operation.parameters ?: return false
        return parameters.any { parameter ->
            parameter.`in` == "header" && parameter.name.equals(headerName, ignoreCase = true)
        }
    }

    companion object {
        private const val AUTH_CONTROLLER_NAME = "AuthController"
        private const val ADMIN_CONTROLLER_PREFIX = "Admin"
        private const val USER_ID_HEADER_NAME = "X-USER-ID"
        private const val ADMIN_TOKEN_HEADER_NAME = "X-ADMIN-TOKEN"
    }
}
