package com.back.back9.global.security

import com.back.back9.domain.user.entity.User
import com.back.back9.domain.user.service.UserService
import com.back.back9.global.exception.ServiceException
import com.back.back9.global.rq.Rq
import com.back.back9.global.rsData.RsData
import com.back.back9.standard.util.Ut
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

@Component
class CustomAuthenticationFilter(
    @Lazy private val userService: UserService,
    private val applicationContext: ApplicationContext
) : OncePerRequestFilter() {

    private fun getRq(request: HttpServletRequest, response: HttpServletResponse): Rq {
        return applicationContext.getBean(Rq::class.java, request, response)
    }

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        logger.debug("Processing request for " + request.requestURI)

        val rq = getRq(request, response)

        try {
            doFilterLogic(request, response, filterChain, rq)
        } catch (e: ServiceException) {
            val rsData = e.rsData as RsData<Unit>
            response.contentType = "application/json"
            response.status = rsData.statusCode()
            response.writer.write(
                Ut.json.toString(rsData)
            )
        } catch (e: Exception) {
            throw e
        }
    }

    @Throws(ServletException::class, IOException::class)
    private fun doFilterLogic(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
        rq: Rq
    ) {
        val requestURI = request.requestURI

        println("=== CustomAuthenticationFilter 요청 처리 ===")
        println("요청 URI: $requestURI")
        println("요청 메소드: ${request.method}")

        if (isAuthenticationBypassed(requestURI)) {
            println("인증 필터 건너뛰기: $requestURI")
            filterChain.doFilter(request, response)
            return
        }

        println("인증 필터 처리 시작: $requestURI")

        val (apiKey, accessToken) = getApiAndAccessToken(rq)

        println("=== 쿠키/헤더 확인 ===")
        println("apiKey: ${apiKey?.let { "존재 - $it" } ?: "없음"}")
        println("accessToken: ${accessToken?.let { "존재 - 길이: ${it.length}" } ?: "없음"}")

        if (apiKey.isNullOrBlank() && accessToken.isNullOrBlank()) {
            println("토큰이 없어서 필터 통과 - 401 오류 발생 예상")
            filterChain.doFilter(request, response)
            return
        }

        var user: User? = null
        var isAccessTokenValid = false

        if (!accessToken.isNullOrBlank()) {
            val payload = userService.getPayloadFromToken(accessToken)
            val id = (payload["id"] as? Number)?.toLong()
            if (id != null) {
                user = userService.findById(id)
                if (user != null) {
                    isAccessTokenValid = true
                }
            }
        }

        if (user == null && !apiKey.isNullOrBlank()) {
            user = userService.findByApiKey(apiKey)
                ?: throw ServiceException("401-3", "API 키가 유효하지 않습니다.")
        }

        user?.let {
            if (!accessToken.isNullOrBlank() && !isAccessTokenValid) {
                val newAccessToken = userService.genAccessToken(it)
                rq.setCookie("accessToken", newAccessToken)
                rq.setHeader("Authorization", "Bearer $newAccessToken")
            }

            val userDetails: UserDetails = SecurityUser(it)
            val authentication = UsernamePasswordAuthenticationToken(
                userDetails,
                userDetails.password,
                userDetails.authorities
            )
            SecurityContextHolder.getContext().authentication = authentication
        }

        filterChain.doFilter(request, response)
    }

    private fun isAuthenticationBypassed(requestURI: String): Boolean {
        if (!requestURI.startsWith("/api/") ||
            requestURI.startsWith("/oauth2/") ||
            requestURI.startsWith("/login/oauth2/") ||
            requestURI.startsWith("/api/auth/")) {
            return true
        }

        val openApiUris = listOf(
            "/api/v1/users/login",
            "/api/v1/users/register",
            "/api/v1/users/logout",
            "/api/v1/users/register-admin"
        )
        return openApiUris.contains(requestURI)
    }

    private fun getApiAndAccessToken(rq: Rq): Pair<String?, String?> {
        val headerAuthorization = rq.getHeader("Authorization", "")

        if (headerAuthorization.isNotBlank()) {
            if (!headerAuthorization.startsWith("Bearer ")) {
                throw ServiceException("401-2", "Authorization 헤더가 Bearer 형식이 아닙니다.")
            }
            val parts = headerAuthorization.split(" ", limit = 3)
            val apiKey = parts.getOrNull(1)
            val accessToken = parts.getOrNull(2)
            return Pair(apiKey, accessToken)
        }

        val apiKey = rq.getCookieValue("apiKey", "")
        val accessToken = rq.getCookieValue("accessToken", "")
        return Pair(apiKey.ifBlank { null }, accessToken.ifBlank { null })
    }
}