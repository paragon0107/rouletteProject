package com.roulette.backend.common.exception

class BusinessException(
    val code: String,
    override val message: String,
) : RuntimeException(message)
