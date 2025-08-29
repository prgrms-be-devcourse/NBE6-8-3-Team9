package com.back.back9.global.security

import com.back.back9.domain.user.dto.UserRegisterDto
import com.back.back9.domain.user.service.UserService
import org.springframework.context.annotation.Lazy
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

@Service
class CustomOAuth2UserService(
    @Lazy private val userService: UserService
) : DefaultOAuth2UserService() {

    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)

        val email: String? = oAuth2User.getAttribute("email")
        val name: String? = oAuth2User.getAttribute("name")

        require(!email.isNullOrBlank()) { "OAuth2 provider must provide an email" }

        val user = userService.findByUserLoginId(email)
            ?: userService.register(
                UserRegisterDto(
                    userLoginId = email,
                    username = name ?: "Unknown",
                    password = "oauth2",
                    confirmPassword = "oauth2"
                )
            ).data

        return SecurityUser(user!!)
    }
}