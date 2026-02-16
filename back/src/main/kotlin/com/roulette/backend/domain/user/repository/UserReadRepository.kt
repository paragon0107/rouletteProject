package com.roulette.backend.domain.user.repository

import com.roulette.backend.domain.user.domain.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserReadRepository : JpaRepository<User, Long> {
    fun findByNickname(nickname: String): User?

    fun existsByNickname(nickname: String): Boolean
}
