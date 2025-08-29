package com.back.back9.global.security

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.stereotype.Component
import org.springframework.util.SerializationUtils
import org.springframework.web.util.WebUtils
import java.util.Base64

@Component
class HttpCookieOAuth2AuthorizationRequestRepository(
    @Value("\${app.oauth2.cookie-domain:}") private val cookieDomain: String = ""
) : AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    companion object {
        const val OAUTH2_AUTH_REQUEST_COOKIE_NAME = "oauth2_auth_request"
        const val REDIRECT_URI_PARAM_COOKIE_NAME = "frontend-url"
        private const val COOKIE_EXPIRE_SECONDS = 180
    }

    override fun loadAuthorizationRequest(request: HttpServletRequest): OAuth2AuthorizationRequest? {
        val cookie = WebUtils.getCookie(request, OAUTH2_AUTH_REQUEST_COOKIE_NAME)
        println("=== OAuth2 쿠키 로드 시도 ===")
        println("요청 URI: ${request.requestURI}")
        println("쿠키 존재 여부: ${cookie != null}")

        return cookie?.let {
            println("쿠키 값: ${it.value}")
            runCatching { deserialize(it.value) }
                .onSuccess { println("역직렬화 성공") }
                .onFailure { e -> println("역직렬화 실패: ${e.message}") }
                .getOrNull()
        } ?: run {
            println("쿠키를 찾을 수 없음")
            request.cookies?.forEach { c -> println("- ${c.name}: ${c.value}") }
            null
        }
    }

    override fun saveAuthorizationRequest(
        authorizationRequest: OAuth2AuthorizationRequest?,
        request: HttpServletRequest,
        response: HttpServletResponse
    ) {
        if (authorizationRequest == null) {
            removeAuthorizationRequestCookies(request, response)
            return
        }

        val serialized = serialize(authorizationRequest)
        val authCookieHeader = buildString {
            append("$OAUTH2_AUTH_REQUEST_COOKIE_NAME=$serialized; Path=/; HttpOnly; Max-Age=$COOKIE_EXPIRE_SECONDS; SameSite=None; Secure")
            if (cookieDomain.isNotEmpty()) append("; Domain=$cookieDomain")
        }
        response.addHeader("Set-Cookie", authCookieHeader)

        request.getParameter("frontend-url")?.takeIf { it.isNotBlank() }?.let { redirectUri ->
            val redirectCookieHeader = buildString {
                append("$REDIRECT_URI_PARAM_COOKIE_NAME=$redirectUri; Path=/; Max-Age=$COOKIE_EXPIRE_SECONDS; SameSite=None; Secure")
                if (cookieDomain.isNotEmpty()) append("; Domain=$cookieDomain")
            }
            response.addHeader("Set-Cookie", redirectCookieHeader)
        }
    }

    // 이 메서드는 거의 사용되지 않으나, 인터페이스 구현을 위해 남김
    /*override fun removeAuthorizationRequest(request: HttpServletRequest): OAuth2AuthorizationRequest? =
        loadAuthorizationRequest(request)*/

    override fun removeAuthorizationRequest(
        request: HttpServletRequest,
        response: HttpServletResponse
    ): OAuth2AuthorizationRequest? {
        val authReq = loadAuthorizationRequest(request)
        removeAuthorizationRequestCookies(request, response)
        return authReq
    }

    private fun removeAuthorizationRequestCookies(request: HttpServletRequest, response: HttpServletResponse) {
        Cookie(OAUTH2_AUTH_REQUEST_COOKIE_NAME, null).apply {
            path = "/"
            isHttpOnly = true
            maxAge = 0
            if (cookieDomain.isNotEmpty()) domain = cookieDomain
            response.addCookie(this)
        }
        var cookieHeader = "$OAUTH2_AUTH_REQUEST_COOKIE_NAME=; Path=/; HttpOnly; Max-Age=0; SameSite=Lax"
        if (cookieDomain.isNotEmpty()) cookieHeader += "; Domain=$cookieDomain"
        response.addHeader("Set-Cookie", cookieHeader)
    }

    private fun serialize(obj: OAuth2AuthorizationRequest): String =
        runCatching {
            val bytes = SerializationUtils.serialize(obj)
            Base64.getUrlEncoder().encodeToString(bytes)
        }.getOrElse { throw IllegalArgumentException("Could not serialize OAuth2AuthorizationRequest", it) }

    private fun deserialize(serialized: String): OAuth2AuthorizationRequest =
        runCatching {
            val bytes = Base64.getUrlDecoder().decode(serialized)
            SerializationUtils.deserialize(bytes) as OAuth2AuthorizationRequest
        }.getOrElse { throw IllegalArgumentException("Could not deserialize OAuth2AuthorizationRequest", it) }
}