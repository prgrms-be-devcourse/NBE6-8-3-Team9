package com.back.back9.domain.user.controller

import com.back.back9.domain.user.dto.UserDto
import com.back.back9.domain.user.dto.UserRegisterDto
import com.back.back9.domain.user.dto.UserLoginReqBody
import com.back.back9.domain.user.dto.UserLoginResBody
import com.back.back9.domain.user.service.UserService
import com.back.back9.global.rq.Rq
import com.back.back9.global.rsData.RsData
import com.back.back9.global.security.HttpCookieOAuth2AuthorizationRequestRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.web.bind.annotation.*
import java.util.function.BiConsumer

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "UserController", description = "API 사용자 컨트롤러")
@SecurityRequirement(name = "bearerAuth")
class UserController(
    private val userService: UserService,
    private val rq: Rq
) {
    private val log = LoggerFactory.getLogger(UserController::class.java)

    @PostMapping("/register")
    @Operation(summary = "회원가입")
    fun register(@Valid @RequestBody reqBody: UserRegisterDto): RsData<UserDto> {
        log.info("회원가입 요청: {}", reqBody)
        val registerResult = userService.register(reqBody)
        log.info("회원가입 결과: {}", registerResult)
        if (!registerResult.resultCode.startsWith("200")) {
            log.warn("회원가입 실패: {}", registerResult)
            return RsData(registerResult.resultCode, registerResult.msg)
        }
        log.info("회원가입 성공: {}", registerResult.data)
        return RsData("201", registerResult.msg, UserDto.from(registerResult.data!!))
    }

    @PostMapping("/register-admin")
    @Operation(summary = "관리자 회원가입")
    fun registerAdmin(@Valid @RequestBody reqBody: UserRegisterDto): RsData<UserDto> {
        val registerResult = userService.registerAdmin(reqBody)
        if (!registerResult.resultCode.startsWith("200")) {
            return RsData(registerResult.resultCode, registerResult.msg)
        }
        return RsData("201", registerResult.msg, UserDto.from(registerResult.data!!))
    }

    @PostMapping("/login")
    @Operation(summary = "로그인")
    fun login(@Valid @RequestBody reqBody: UserLoginReqBody): RsData<UserLoginResBody> {
        val actor = rq.getActor()
        val apiKeyCookie = rq.getCookieValue("apiKey", null)
        log.info("로그인 요청: userLoginId={}, actor={}, apiKeyCookie={}", reqBody.userLoginId, actor, apiKeyCookie)
        if (actor != null || (apiKeyCookie != null && userService.findByApiKey(apiKeyCookie) != null)) {
            log.warn("이미 로그인된 상태입니다. actor={}, apiKeyCookie={}", actor, apiKeyCookie)
            return RsData("400", "이미 로그인된 상태입니다.")
        }

        val loginResult = userService.login(reqBody.userLoginId, reqBody.password)
        log.info("로그인 결과: {}", loginResult)
        if (!loginResult.resultCode.startsWith("200")) {
            log.warn("로그인 실패: {}", loginResult)
            return RsData(loginResult.resultCode, loginResult.msg)
        }

        val user = loginResult.data!!
        val accessToken = userService.genAccessToken(user)

        log.info("쿠키 세팅: apiKey={}, accessToken={}, role={}", user.apiKey, accessToken, user.role.name)
        rq.setCookie("apiKey", user.apiKey)
        rq.setCookie("accessToken", accessToken)
        rq.setCookie("role", user.role.name)

        return RsData(
            "200-1",
            "${user.username}님 환영합니다.",
            UserLoginResBody(UserDto.from(user), user.apiKey!!, accessToken)
        )
    }

    @DeleteMapping("/logout")
    @Operation(summary = "통합 로그아웃 (OAuth + JWT/쿠키)")
    fun logout(request: HttpServletRequest, response: HttpServletResponse): RsData<Void> {
        // 1) OAuth 세션 무효화
        val authentication: Authentication? = SecurityContextHolder.getContext().authentication
        if (authentication != null && authentication.isAuthenticated) {
            SecurityContextLogoutHandler().logout(request, response, authentication)
            log.info("OAuth 세션 사용자 로그아웃 처리 완료")
        }

        // --- 공통 삭제용 헬퍼 람다 ---
        val deleteCookieBoth = BiConsumer<String, String> { name, path ->
            val base = "$name=; Path=$path; Max-Age=0; SameSite=None; Secure; HttpOnly"
            response.addHeader(HttpHeaders.SET_COOKIE, base)
            response.addHeader(
                HttpHeaders.SET_COOKIE,
                "$base; Domain=d64t5u28gt0rl.cloudfront.net"
            )
        }

        // 2) JSESSIONID 만료
        deleteCookieBoth.accept("JSESSIONID", "/")

        // 3) JWT 쿠키들 삭제
        deleteCookieBoth.accept("apiKey", "/")
        deleteCookieBoth.accept("accessToken", "/")
        deleteCookieBoth.accept("role", "/")

        // 4) OAuth2 요청용 쿠키들 삭제
        deleteCookieBoth.accept(
            HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_AUTH_REQUEST_COOKIE_NAME,
            "/"
        )
        deleteCookieBoth.accept(
            HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME,
            "/"
        )

        // 5) SecurityContext 클리어
        SecurityContextHolder.clearContext()
        log.info("통합 로그아웃 처리 완료")

        return RsData("200-1", "로그아웃 되었습니다.")
    }

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회")
    fun me(): RsData<UserDto> {
        val actor = rq.getActor()
        log.info("내 정보 조회 요청: actor={}", actor)
        if (actor == null) {
            log.warn("로그인 필요: actor=null")
            return RsData("401", "로그인이 필요합니다.")
        }
        log.info("내 정보 조회 성공: {}", actor)
        return RsData("200", "현재 사용자 정보입니다.", UserDto.from(actor))
    }
}