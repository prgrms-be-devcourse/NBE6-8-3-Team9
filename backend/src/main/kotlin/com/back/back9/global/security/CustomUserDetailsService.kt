package com.back.back9.global.security

import com.back.back9.domain.user.service.UserService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val userService: UserService
) : UserDetailsService {

    override fun loadUserByUsername(userLoginId: String): UserDetails {
        val user = userService.findByUserLoginId(userLoginId)
            ?: throw UsernameNotFoundException("해당 사용자를 찾을 수 없습니다: $userLoginId")
        return SecurityUser(user)
    }
}
