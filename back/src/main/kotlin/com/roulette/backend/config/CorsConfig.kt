package com.roulette.backend.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class CorsConfig : WebMvcConfigurer {
    override fun addCorsMappings(corsRegistry: CorsRegistry) {
        corsRegistry
            .addMapping(ALL_PATH_PATTERN)
            // 어떤 Origin 요청이든 허용하면서 자격 증명 헤더 전송도 가능하게 처리합니다.
            .allowedOriginPatterns(ALL_PATTERN)
            .allowedMethods(ALL_PATTERN)
            .allowedHeaders(ALL_PATTERN)
            .exposedHeaders(ALL_PATTERN)
            .allowCredentials(true)
            .maxAge(PREFLIGHT_CACHE_SECONDS)
    }

    companion object {
        private const val ALL_PATH_PATTERN = "/**"
        private const val ALL_PATTERN = "*"
        private const val PREFLIGHT_CACHE_SECONDS = 3600L
    }
}
