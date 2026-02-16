package com.roulette.backend.domain.user.service

import com.roulette.backend.common.exception.BusinessException
import com.roulette.backend.domain.user.domain.User
import com.roulette.backend.domain.user.domain.UserRole
import com.roulette.backend.domain.user.repository.UserReadRepository
import com.roulette.backend.domain.user.repository.UserWriteRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MockLoginUseCase(
    private val userReadRepository: UserReadRepository,
    private val userWriteRepository: UserWriteRepository,
) {
    @Transactional
    fun execute(nickname: String): MockLoginResult {
        val trimmedNickname = nickname.trim()
        validateNickname(trimmedNickname)
        val existingUser = userReadRepository.findByNickname(trimmedNickname)
        if (existingUser != null) return existingUser.toMockLoginResult()

        val createdUserId = insertOrFindUserId(trimmedNickname)
        val createdUser = userReadRepository.findById(createdUserId)
            .orElseThrow { BusinessException("USER_NOT_FOUND", "생성된 사용자를 찾을 수 없습니다.") }
        return createdUser.toMockLoginResult()
    }

    private fun validateNickname(candidate: String) {
        val minLength = User.MIN_NICKNAME_LENGTH
        val maxLength = User.MAX_NICKNAME_LENGTH
        if (candidate.length in minLength..maxLength) return
        throw BusinessException("USER_INVALID_NICKNAME", "닉네임은 $minLength~${maxLength}자여야 합니다.")
    }

    private fun insertOrFindUserId(nickname: String): Long {
        return try {
            userWriteRepository.insertUser(nickname, UserRole.USER)
        } catch (_: DataIntegrityViolationException) {
            val existingUser = userReadRepository.findByNickname(nickname)
                ?: throw BusinessException("USER_CONFLICT", "동시 로그인 처리 중 사용자 조회에 실패했습니다.")
            requireNotNull(existingUser.id)
        }
    }
}

data class MockLoginResult(
    val userId: Long,
    val nickname: String,
    val role: UserRole,
)

private fun User.toMockLoginResult(): MockLoginResult {
    val safeUserId = requireNotNull(id) { "사용자 식별자가 없습니다." }
    return MockLoginResult(
        userId = safeUserId,
        nickname = nickname,
        role = role,
    )
}
