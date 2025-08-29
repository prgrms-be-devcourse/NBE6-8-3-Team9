package com.back.back9.global.rq

import com.back.back9.domain.user.entity.User
import com.back.back9.global.security.SecurityUser
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import java.time.Duration

@Component
class Rq(
    private val req: HttpServletRequest,
    private val resp: HttpServletResponse,
    @Value("\${app.cookie.same-site:Lax}")
    private val cookieSameSite: String,
    @Value("\${app.cookie.domain:}")
    private val cookieDomain: String,
    @Value("\${app.cookie.secure:true}")
    private val cookieSecure: Boolean,
    @Value("\${app.cookie.max-age-seconds:31536000}")
    private val cookieMaxAgeSeconds: Long
) {

    /** 현재 인증된 사용자 반환(null 허용) */
    fun getActor(): User? {
        val auth: Authentication? = SecurityContextHolder.getContext().authentication
        if (auth == null || !auth.isAuthenticated) return null
        val principal = auth.principal
        return if (principal is SecurityUser) principal.user else null
    }

    /** 요청 헤더 조회(빈 문자열은 default 반환) */
    fun getHeader(name: String, defaultValue: String): String {
        val v = req.getHeader(name)
        return if (StringUtils.hasText(v)) v else defaultValue
    }

    /** 응답 헤더 세팅(비우면 빈 값으로 세팅) */
    fun setHeader(name: String, value: String?) {
        resp.setHeader(name, value ?: "")
    }

    /** 요청 쿠키 값 조회 (널러블 지원) */
    fun getCookieValue(name: String, defaultValue: String?): String? {
        val cookies: Array<Cookie>? = req.cookies
        if (cookies == null) return defaultValue
        return cookies.firstOrNull { it.name == name }
            ?.value
            ?.takeIf { StringUtils.hasText(it) }
            ?: defaultValue
    }

    /** HttpOnly 쿠키 설정(기본 maxAge=설정값) */
    fun setCookie(name: String, value: String?) {
        setCookie(name, value, Duration.ofSeconds(cookieMaxAgeSeconds), true, "/")
    }

    /** 쿠키 삭제(설정 때와 동일 속성으로) */
    fun deleteCookie(name: String) {
        setCookie(name, null, Duration.ZERO, true, "/")
    }

    /** 필요 시 HttpOnly/Path/MaxAge 커스텀 */
    fun setCookie(name: String, value: String?, maxAge: Duration, httpOnly: Boolean, path: String?) {
        val delete = !StringUtils.hasText(value)
        val builder = ResponseCookie.from(name, if (delete) "" else value ?: "")
            .httpOnly(httpOnly)
            .secure(cookieSecure)
            .path(if (path.isNullOrBlank()) "/" else path)
            .sameSite(cookieSameSite)

        if (StringUtils.hasText(cookieDomain)) {
            builder.domain(cookieDomain)
        }

        builder.maxAge(if (delete) Duration.ZERO else maxAge)
        resp.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString())
    }

    /** 현재 요청이 HTTPS 인지 단순 조회 (프록시 헤더 포함) */
    fun isHttps(): Boolean {
        val proto = req.getHeader("X-Forwarded-Proto")
        return if (StringUtils.hasText(proto)) {
            "https".equals(proto, ignoreCase = true)
        } else {
            "https".equals(req.scheme, ignoreCase = true)
        }
    }

    /** 디버그용: 쿠키 정책 문자열 */
    fun cookiePolicyDebug(): String =
        "domain=${if (StringUtils.hasText(cookieDomain)) cookieDomain else "(host-only)"}" +
                ", secure=$cookieSecure" +
                ", sameSite=$cookieSameSite" +
                ", maxAgeSec=$cookieMaxAgeSeconds"
}