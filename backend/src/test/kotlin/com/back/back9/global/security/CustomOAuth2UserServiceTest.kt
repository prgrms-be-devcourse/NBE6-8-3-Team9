package com.back.back9.global.security

import com.back.back9.domain.user.entity.User
import com.back.back9.domain.user.service.UserService
import com.back.back9.global.rsData.RsData
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User

class CustomOAuth2UserServiceTest {

    class TestCustomOAuth2UserService(
        userService: UserService,
        private val attributes: Map<String, Any?>
    ) : CustomOAuth2UserService(userService) {
        private val userService: UserService = userService

        override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
            val email = attributes["email"] as String
            val name = attributes["name"] as String

            val userOpt: User? = userService.findByUserLoginId(email)
            val user: User = if (userOpt == null) {
                val newUser = User.builder()
                    .userLoginId(email)
                    .username(name)
                    .password("oauth2")
                    .role(User.UserRole.MEMBER)
                    .apiKey("dummy-api-key")
                    .build()
                userService.register(any())
                newUser
            } else {
                userOpt
            }
            return SecurityUser(user)
        }
    }

    @Test
    @DisplayName("OAuth2 로그인 시 신규 사용자는 DB에 저장된다")
    fun testOAuth2UserRegistration() {
        // given
        val userService = Mockito.mock(UserService::class.java)

        val attributes = mapOf(
            "email" to "testuser@example.com",
            "name" to "테스트유저"
        )

        Mockito.`when`(userService.findByUserLoginId("testuser@example.com"))
            .thenReturn(null)

        val newUser = User.builder()
            .userLoginId("testuser@example.com")
            .username("테스트유저")
            .password("oauth2")
            .role(User.UserRole.MEMBER)
            .apiKey("dummy-api-key")
            .build()

        Mockito.`when`(userService.register(any()))
            .thenReturn(RsData("200", "ok", newUser))

        val customOAuth2UserService = TestCustomOAuth2UserService(userService, attributes)
        val userRequest = Mockito.mock(OAuth2UserRequest::class.java)

        // when
        val result = customOAuth2UserService.loadUser(userRequest)

        // then
        assertThat(result).isInstanceOf(SecurityUser::class.java)
        val securityUser = result as SecurityUser
        assertThat(securityUser.user.userLoginId).isEqualTo("testuser@example.com")
        assertThat(securityUser.user.username).isEqualTo("테스트유저")
    }
}