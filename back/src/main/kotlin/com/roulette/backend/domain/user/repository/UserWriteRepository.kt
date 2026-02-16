package com.roulette.backend.domain.user.repository

import com.roulette.backend.common.exception.BusinessException
import com.roulette.backend.domain.user.domain.UserRole
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneOffset

@Repository
class UserWriteRepository(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {
    fun insertUser(
        nickname: String,
        role: UserRole,
    ): Long {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val parameters = MapSqlParameterSource()
            .addValue("nickname", nickname.trim())
            .addValue("role", role.name)
            .addValue("createdAt", now)
            .addValue("updatedAt", now)
        val keyHolder = GeneratedKeyHolder()
        namedParameterJdbcTemplate.update(SQL_INSERT_USER, parameters, keyHolder, arrayOf("id"))
        return keyHolder.key?.toLong()
            ?: throw BusinessException("USER_INSERT_FAILED", "사용자 생성에 실패했습니다.")
    }

    fun updateUserRole(
        userId: Long,
        role: UserRole,
    ): Int {
        val parameters = MapSqlParameterSource()
            .addValue("id", userId)
            .addValue("role", role.name)
            .addValue("updatedAt", LocalDateTime.now(ZoneOffset.UTC))
        return namedParameterJdbcTemplate.update(SQL_UPDATE_USER_ROLE, parameters)
    }

    companion object {
        private const val SQL_INSERT_USER = """
            INSERT INTO users (nickname, role, created_at, updated_at)
            VALUES (:nickname, :role, :createdAt, :updatedAt)
        """

        private const val SQL_UPDATE_USER_ROLE = """
            UPDATE users
            SET role = :role,
                updated_at = :updatedAt
            WHERE id = :id
        """
    }
}
