package com.roulette.backend.common.response

import com.fasterxml.jackson.annotation.JsonProperty

data class ApiErrorResponse(
    val error: ErrorBody,
)

data class ErrorBody(
    val code: String,
    val message: String,
    val details: List<ErrorDetail>,
    @JsonProperty("trace_id")
    val traceId: String,
)

data class ErrorDetail(
    val field: String,
    val reason: String,
)
