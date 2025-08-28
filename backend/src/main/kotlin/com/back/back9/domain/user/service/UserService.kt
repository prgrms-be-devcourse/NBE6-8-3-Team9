package com.back.back9.domain.user.service

import com.back.back9.domain.user.dto.UserRegisterDto
import com.back.back9.domain.user.entity.User
import com.back.back9.domain.user.repository.UserRepository
import com.back.back9.domain.wallet.service.WalletService
import com.back.back9.global.exception.ServiceException
import com.back.back9.global.rsData.RsData
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authTokenService: AuthTokenService,
    private val walletService: WalletService
) {

    private fun registerUser(dto: UserRegisterDto, role: User.UserRole): RsData<User> {
        if (dto.password != dto.confirmPassword) {
            return RsData("400", "비밀번호가 일치하지 않습니다.")
        }
        if (userRepository.findByUserLoginId(dto.userLoginId) != null) {
            return RsData("400-1", "이미 존재하는 아이디입니다.")
        }
        if (userRepository.findByUsername(dto.username) != null) {
            return RsData("400-2", "이미 존재하는 유저이름입니다.")
        }

        val user = User(
            userLoginId = dto.userLoginId,
            username = dto.username,
            password = passwordEncoder.encode(dto.password),
            role = role,
            apiKey = UUID.randomUUID().toString()
        )

        val savedUser = userRepository.save(user)

        savedUser.id?.let { walletService.createWallet(it) }

        val message = if (role == User.UserRole.ADMIN) "관리자 회원가입이 완료되었습니다." else "회원가입이 완료되었습니다."
        return RsData("200-1", message, savedUser)
    }

    @Transactional
    fun register(dto: UserRegisterDto): RsData<User> {
        return registerUser(dto, User.UserRole.MEMBER)
    }

    @Transactional
    fun registerAdmin(dto: UserRegisterDto): RsData<User> {
        return registerUser(dto, User.UserRole.ADMIN)
    }

    @Transactional(readOnly = true)
    fun login(userLoginId: String, password: String): RsData<User> {
        val user = userRepository.findByUserLoginId(userLoginId)
            ?: return RsData("401-1", "존재하지 않는 아이디입니다.")

        if (!passwordEncoder.matches(password, user.password)) {
            return RsData("401-2", "비밀번호가 일치하지 않습니다.")
        }

        user.id?.let {
            if (!walletService.existsByUserId(it)) {
                walletService.createWallet(it)
            }
        }

        return RsData("200-1", "로그인 성공", user)
    }

    @Transactional(readOnly = true)
    fun findByUserLoginId(userLoginId: String): User? {
        return userRepository.findByUserLoginId(userLoginId)
    }

    @Transactional
    fun deleteByUserLoginId(userLoginId: String) {
        val user = userRepository.findByUserLoginId(userLoginId)
            ?: throw ServiceException("404", "해당 아이디의 사용자가 없습니다.")
        user.id?.let { walletService.deleteWalletByUserId(it) }
        userRepository.delete(user)
    }

    @Transactional(readOnly = true)
    fun findByApiKey(apiKey: String): User? {
        return userRepository.findByApiKey(apiKey)
    }

    @Transactional(readOnly = true)
    fun searchByUsername(keyword: String): List<User> {
        return userRepository.findByUsernameContaining(keyword)
    }

    @Transactional(readOnly = true)
    fun findAll(): List<User> {
        return userRepository.findAll()
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): User? {
        return userRepository.findByIdOrNull(id)
    }

    fun getPayloadFromToken(accessToken: String): Map<String, Any> {
        return authTokenService.payload(accessToken)?.filterValues { it != null } as? Map<String, Any> ?: emptyMap()
    }

    fun genAccessToken(user: User): String {
        return authTokenService.genAccessToken(user)
    }

    fun save(user: User): User {
        return userRepository.save(user)
    }
}
