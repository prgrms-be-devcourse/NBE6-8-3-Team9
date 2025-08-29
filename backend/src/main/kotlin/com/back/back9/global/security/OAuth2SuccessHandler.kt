package com.back.back9.global.security

import com.back.back9.domain.user.repository.UserRepository
import com.back.back9.domain.user.service.UserService
import com.back.back9.domain.wallet.service.WalletService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class OAuth2SuccessHandler(
    @Lazy private val userService: UserService,
    @Lazy private val walletService: WalletService,
    @Lazy private val userRepository: UserRepository,
    @Value("\${spring.profiles.active:dev}") private val activeProfile: String,
    @Value("\${app.oauth2.frontend-url}") private val frontendUrl: String,
    @Value("\${app.oauth2.cookie-domain:}") private val cookieDomain: String
) : AuthenticationSuccessHandler {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        println("=== OAuth2SuccessHandler 시작 ===")
        println("활성 프로파일: $activeProfile")

        val securityUser = authentication.principal as SecurityUser
        var user = securityUser.user

        println("인증된 사용자: ${user.userLoginId}")

        if (user.id == null) {
            println("사용자 ID가 null이므로 저장 필요")
            user = userRepository.save(user)
            println("사용자 저장 완료, ID: ${user.id}")
        }

        val accessToken = userService.genAccessToken(user)
        val role = user.role.name
        val apiKey = user.apiKey

        println("액세스 토큰 생성 완료")
        println("사용자 역할: $role")

        if (!walletService.existsByUserId(user.id)) {
            println("지갑이 존재하지 않아 생성")
            walletService.createWallet(user.id)
        } else {
            println("지갑이 이미 존재함")
        }

        println("=== 쿠키 설정 시작 ===")
        println("accessToken: $accessToken")
        println("apiKey: $apiKey")
        println("role: $role")

        val isLocal = activeProfile == "dev"

        fun setCookie(name: String, value: String, extra: String) {
            response.addHeader("Set-Cookie", "$name=$value; Path=/; $extra")
        }

        if (isLocal) {
            println("로컬 환경 - 일반 쿠키 설정")
            val extra = "HttpOnly=true; SameSite=Lax; Max-Age=3600"
            setCookie("accessToken", accessToken, extra)
            setCookie("apiKey", apiKey ?: "", extra)
            setCookie("role", role, extra)
        } else {
            println("프로덕션 환경 - 크로스 도메인 쿠키 설정")
            val extra = "HttpOnly=true; Secure=true; SameSite=None; Domain=$cookieDomain"
            setCookie("accessToken", accessToken, extra)
            setCookie("apiKey", apiKey ?: "", extra)
            setCookie("role", role, extra)
        }

        println("Set-Cookie 헤더 추가 완료")
        println("=== 응답 헤더 확인 ===")
        response.headerNames.filter { it.equals("Set-Cookie", true) }
            .forEach { headerName ->
                response.getHeaders(headerName).forEach { println("Set-Cookie: $it") }
            }

        val redirectUrl = "$frontendUrl/api/auth/google/callback?token=$accessToken&apiKey=$apiKey&role=$role"

        if (isLocal) {
            println("로컬 환경 - nginx 8888 포트 연결")
        } else {
            println("프로덕션 환경 - API 라우트 활용")
        }

        println("리다이렉트 URL: $redirectUrl")

        try {
            response.sendRedirect(redirectUrl)
            println("리다이렉트 완료")
        } catch (e: Exception) {
            println("리다이렉트 실패: ${e.message}")
            e.printStackTrace()
        }
    }
}