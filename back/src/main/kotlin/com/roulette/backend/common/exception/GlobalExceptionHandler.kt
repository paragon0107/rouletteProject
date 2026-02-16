package com.roulette.backend.common.exception

import com.roulette.backend.common.response.ApiErrorResponse
import com.roulette.backend.common.response.ErrorBody
import com.roulette.backend.common.response.ErrorDetail
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.util.UUID

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(
        exception: BusinessException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> {
        val status = resolveStatus(exception.code)
        val traceId = resolveTraceId(request)
        logBusinessException(exception, request, status, traceId)
        val responseBody = ApiErrorResponse(
            error = ErrorBody(
                code = exception.code,
                message = exception.message,
                details = emptyList(),
                traceId = traceId,
            ),
        )
        return ResponseEntity.status(status).body(responseBody)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(
        exception: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> {
        val details = exception.bindingResult.fieldErrors.map { error ->
            ErrorDetail(field = error.field, reason = error.defaultMessage ?: "입력값이 유효하지 않습니다.")
        }
        val traceId = resolveTraceId(request)
        logInvalidRequestException(
            exceptionType = MethodArgumentNotValidException::class.simpleName.orEmpty(),
            request = request,
            traceId = traceId,
            detailCount = details.size,
        )
        val responseBody = ApiErrorResponse(
            error = ErrorBody(
                code = COMMON_INVALID_REQUEST_CODE,
                message = COMMON_INVALID_REQUEST_MESSAGE,
                details = details,
                traceId = traceId,
            ),
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(
        exception: ConstraintViolationException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> {
        val details = exception.constraintViolations.map { violation ->
            ErrorDetail(field = violation.propertyPath.toString(), reason = violation.message)
        }
        val traceId = resolveTraceId(request)
        logInvalidRequestException(
            exceptionType = ConstraintViolationException::class.simpleName.orEmpty(),
            request = request,
            traceId = traceId,
            detailCount = details.size,
        )
        val responseBody = ApiErrorResponse(
            error = ErrorBody(
                code = COMMON_INVALID_REQUEST_CODE,
                message = COMMON_INVALID_REQUEST_MESSAGE,
                details = details,
                traceId = traceId,
            ),
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody)
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(
        exception: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> {
        val traceId = resolveTraceId(request)
        logUnexpectedException(exception, request, traceId)
        val responseBody = ApiErrorResponse(
            error = ErrorBody(
                code = "INTERNAL_SERVER_ERROR",
                message = "서버 처리 중 오류가 발생했습니다.",
                details = listOf(ErrorDetail(field = "internal", reason = exception.javaClass.simpleName)),
                traceId = traceId,
            ),
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseBody)
    }

    private fun logBusinessException(
        exception: BusinessException,
        request: HttpServletRequest,
        status: HttpStatus,
        traceId: String,
    ) {
        logger.warn(
            "업무 예외 발생 code={}, status={}, method={}, uri={}, traceId={}, message={}",
            exception.code,
            status.value(),
            request.method,
            request.requestURI,
            traceId,
            exception.message,
        )
    }

    private fun logInvalidRequestException(
        exceptionType: String,
        request: HttpServletRequest,
        traceId: String,
        detailCount: Int,
    ) {
        logger.warn(
            "유효성 예외 발생 type={}, method={}, uri={}, traceId={}, detailCount={}",
            exceptionType,
            request.method,
            request.requestURI,
            traceId,
            detailCount,
        )
    }

    private fun logUnexpectedException(
        exception: Exception,
        request: HttpServletRequest,
        traceId: String,
    ) {
        logger.error(
            "예상하지 못한 예외 발생 method={}, uri={}, traceId={}, exceptionType={}, message={}",
            request.method,
            request.requestURI,
            traceId,
            exception.javaClass.name,
            exception.message,
            exception,
        )
    }

    private fun resolveStatus(code: String): HttpStatus {
        return when (code) {
            "AUTH_UNAUTHORIZED" -> HttpStatus.UNAUTHORIZED
            "AUTH_FORBIDDEN" -> HttpStatus.FORBIDDEN
            "USER_NOT_FOUND",
            "PRODUCT_NOT_FOUND",
            "ORDER_NOT_FOUND",
            "ROULETTE_PARTICIPATION_NOT_FOUND"
            -> HttpStatus.NOT_FOUND
            "ROULETTE_ALREADY_PARTICIPATED_TODAY",
            "BUDGET_INSUFFICIENT",
            "BUDGET_TOTAL_LESS_THAN_USED",
            "PRODUCT_STOCK_INSUFFICIENT",
            "ORDER_ALREADY_CANCELED",
            "ORDER_CANCEL_NOT_ALLOWED",
            "ORDER_STATUS_CHANGE_NOT_ALLOWED",
            "ROULETTE_PARTICIPATION_ALREADY_CANCELED",
            "POINT_BALANCE_INSUFFICIENT",
            "POINT_ALREADY_USED",
            "PRODUCT_CONFLICT"
            -> HttpStatus.CONFLICT
            else -> HttpStatus.BAD_REQUEST
        }
    }

    private fun resolveTraceId(request: HttpServletRequest): String {
        val traceIdFromHeader = request.getHeader(HEADER_TRACE_ID)?.trim()
        if (!traceIdFromHeader.isNullOrEmpty()) return traceIdFromHeader
        return UUID.randomUUID().toString()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
        private const val HEADER_TRACE_ID = "X-TRACE-ID"
        private const val COMMON_INVALID_REQUEST_CODE = "COMMON_INVALID_REQUEST"
        private const val COMMON_INVALID_REQUEST_MESSAGE = "요청 파라미터가 유효하지 않습니다."
    }
}
