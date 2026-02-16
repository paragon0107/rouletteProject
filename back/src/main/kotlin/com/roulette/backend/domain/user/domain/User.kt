package com.roulette.backend.domain.user.domain

import com.roulette.backend.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "users",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_users_nickname", columnNames = ["nickname"]),
    ],
)
class User(
    nickname: String,
    role: UserRole = UserRole.USER,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "nickname", nullable = false, length = MAX_NICKNAME_LENGTH)
    var nickname: String = nickname.trim()
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    var role: UserRole = role
        protected set

    init {
        validateNickname(this.nickname)
    }

    fun changeNickname(newNickname: String) {
        val trimmedNickname = newNickname.trim()
        validateNickname(trimmedNickname)
        nickname = trimmedNickname
    }

    fun promoteToAdmin() {
        role = UserRole.ADMIN
    }

    fun demoteToUser() {
        role = UserRole.USER
    }

    private fun validateNickname(candidate: String) {
        require(candidate.length in MIN_NICKNAME_LENGTH..MAX_NICKNAME_LENGTH) {
            "닉네임은 $MIN_NICKNAME_LENGTH~${MAX_NICKNAME_LENGTH}자여야 합니다."
        }
    }

    companion object {
        const val MIN_NICKNAME_LENGTH: Int = 2
        const val MAX_NICKNAME_LENGTH: Int = 30
    }
}
