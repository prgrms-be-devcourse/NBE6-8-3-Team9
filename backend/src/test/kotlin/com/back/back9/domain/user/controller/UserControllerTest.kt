package com.back.back9.domain.user.controller

import com.back.back9.domain.common.vo.money.Money
import com.back.back9.domain.user.dto.UserRegisterDto
import com.back.back9.domain.user.service.UserService
import com.back.back9.domain.wallet.repository.WalletRepository
import com.back.back9.domain.wallet.entity.Wallet
import org.assertj.core.api.Assertions
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserControllerTest @Autowired constructor(
    private val mvc: MockMvc,
    private val userService: UserService,
    private val walletRepository: WalletRepository
) {

    @BeforeEach
    fun setUp() {
        userService.register(
            UserRegisterDto(
                "testuser",
                "테스트 사용자",
                "12345678",
                "12345678"
            )
        )
    }

    @Test
    @DisplayName("회원가입")
    fun t1() {
        val resultActions = mvc.perform(
                MockMvcRequestBuilders.post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                   "userLoginId": "testuser1",
                                   "username": "테스트 사용자1",
                                   "password": "12345678",
                                   "confirmPassword": "12345678"
                                 }
                                """.trimIndent()
                        )
        ).andDo(MockMvcResultHandlers.print())

        val user = userService.findByUserLoginId("testuser1")
        Assertions.assertThat(user).isNotNull

        resultActions
                .andExpect(MockMvcResultMatchers.handler().handlerType(UserController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("register"))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("\$.code").value(201))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.message").value("회원가입이 완료되었습니다."))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.result.id").value(user!!.id))
            .andExpect(MockMvcResultMatchers.jsonPath("\$.result.userLoginId").value(user.userLoginId))
    }

    @Test
    @DisplayName("로그인")
    fun t2() {
        val resultActions = mvc.perform(
                MockMvcRequestBuilders.post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                    "userLoginId": "testuser",
                                    "password": "12345678"
                                }
                                """.trimIndent()
                        )
        ).andDo(MockMvcResultHandlers.print())

        val user = userService.findByUserLoginId("testuser")
        Assertions.assertThat(user).isNotNull

        resultActions
                .andExpect(MockMvcResultMatchers.handler().handlerType(UserController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("login"))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("\$.code").value(200))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.message").value(user!!.username + "님 환영합니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("\$.result").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("\$.result.item.id").value(user.id))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.result.apiKey").value(user.apiKey))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.result.accessToken").isNotEmpty)

        resultActions.andExpect { result ->
                val apiKeyCookie = result.response.getCookie("apiKey")
            val accessTokenCookie = result.response.getCookie("accessToken")
            Assertions.assertThat(apiKeyCookie).isNotNull
            Assertions.assertThat(accessTokenCookie).isNotNull

            Assertions.assertThat(apiKeyCookie!!.value).isEqualTo(user.apiKey)
            Assertions.assertThat(apiKeyCookie.path).isEqualTo("/")
            Assertions.assertThat(apiKeyCookie.isHttpOnly).isTrue

            Assertions.assertThat(accessTokenCookie!!.value).isNotBlank()
            Assertions.assertThat(accessTokenCookie.path).isEqualTo("/")
            Assertions.assertThat(accessTokenCookie.isHttpOnly).isTrue
        }
    }

    @Test
    @DisplayName("내 정보 조회")
    fun t3() {
        val loginResult = mvc.perform(
                MockMvcRequestBuilders.post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                    {
                                        "userLoginId": "testuser",
                                        "password": "12345678"
                                    }
                                """.trimIndent()
                        )
        ).andDo(MockMvcResultHandlers.print())

        val apiKeyCookie = loginResult.andReturn().response.getCookie("apiKey")
        val accessTokenCookie = loginResult.andReturn().response.getCookie("accessToken")

        Assertions.assertThat(apiKeyCookie).isNotNull
        Assertions.assertThat(accessTokenCookie).isNotNull

        val actor = userService.findByUserLoginId("testuser")
        Assertions.assertThat(actor).isNotNull

        val resultActions = mvc.perform(
                MockMvcRequestBuilders.get("/api/v1/users/me")
                        .cookie(apiKeyCookie, accessTokenCookie)
        ).andDo(MockMvcResultHandlers.print())

        resultActions
                .andExpect(MockMvcResultMatchers.handler().handlerType(UserController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("me"))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("\$.code").value(200))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.message").value("현재 사용자 정보입니다."))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.result.id").value(actor!!.id))
            .andExpect(MockMvcResultMatchers.jsonPath("\$.result.userLoginId").value(actor.userLoginId))
    }

    @Test
    @DisplayName("로그아웃")
    fun t4() {
        val resultActions = mvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/users/logout")
        ).andDo(MockMvcResultHandlers.print())

        resultActions
                .andExpect(MockMvcResultMatchers.handler().handlerType(UserController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("logout"))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("\$.code").value(200))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.message").value("로그아웃 되었습니다."))
                .andExpect { result ->
                val apiKey = result.response.getCookie("apiKey")
            val accessToken = result.response.getCookie("accessToken")
            Assertions.assertThat(apiKey).isNotNull
            Assertions.assertThat(accessToken).isNotNull

            Assertions.assertThat(apiKey!!.value).isEmpty()
            Assertions.assertThat(apiKey.maxAge).isEqualTo(0)
            Assertions.assertThat(apiKey.path).isEqualTo("/")
            Assertions.assertThat(apiKey.isHttpOnly).isTrue

            Assertions.assertThat(accessToken!!.value).isEmpty()
            Assertions.assertThat(accessToken.maxAge).isEqualTo(0)
            Assertions.assertThat(accessToken.path).isEqualTo("/")
            Assertions.assertThat(accessToken.isHttpOnly).isTrue
        }
    }

    @Test
    @DisplayName("Authorization 헤더가 잘못된 형식일 때 오류 발생")
    fun t5() {
        val resultActions = mvc.perform(
                MockMvcRequestBuilders.get("/api/v1/users/me")
                        .header("Authorization", "invalid-format")
        ).andDo(MockMvcResultHandlers.print())

        resultActions
                .andExpect(MockMvcResultMatchers.status().isUnauthorized)
                .andExpect(MockMvcResultMatchers.jsonPath("\$.code").value(401))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.message").value("Authorization 헤더가 Bearer 형식이 아닙니다."))
    }

    @Test
    @DisplayName("구글 OAuth2 로그인 엔드포인트 접근 시 구글 로그인 창으로 리다이렉트된다")
    fun t6() {
        mvc.perform(MockMvcRequestBuilders.get("/oauth2/authorize/google"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection)
                .andExpect(MockMvcResultMatchers.header().string("Location", Matchers.startsWith("https://accounts.google.com/")))
    }

    @Test
    @DisplayName("로그인 시 쿠키가 정상적으로 발급된다")
    fun t7() {
        val resultActions = mvc.perform(
                MockMvcRequestBuilders.post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                            {
                                "userLoginId": "testuser",
                                "password": "12345678"
                            }
                        """.trimIndent()
                        )
        ).andDo(MockMvcResultHandlers.print())

        val apiKeyCookie = resultActions.andReturn().response.getCookie("apiKey")
        val accessTokenCookie = resultActions.andReturn().response.getCookie("accessToken")

        Assertions.assertThat(apiKeyCookie).isNotNull
        Assertions.assertThat(accessTokenCookie).isNotNull

        Assertions.assertThat(apiKeyCookie!!.value).isNotBlank()
        Assertions.assertThat(accessTokenCookie!!.value).isNotBlank()

        Assertions.assertThat(apiKeyCookie.path).isEqualTo("/")
        Assertions.assertThat(accessTokenCookie.path).isEqualTo("/")

        Assertions.assertThat(apiKeyCookie.isHttpOnly).isTrue
        Assertions.assertThat(accessTokenCookie.isHttpOnly).isTrue
    }

    @Test
    @DisplayName("회원가입 시 지갑이 자동 생성된다")
    fun t8() {
        // 회원가입 요청
        mvc.perform(
                        MockMvcRequestBuilders.post("/api/v1/users/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                    {
                                       "userLoginId": "walletuser",
                                       "username": "지갑유저",
                                       "password": "12345678",
                                       "confirmPassword": "12345678"
                                     }
                                    """.trimIndent()
                                )
                ).andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk)

        // 회원 정보 조회
        val user = userService.findByUserLoginId("walletuser")
                ?: throw RuntimeException()

        // 지갑 정보 조회
        val wallet: Wallet? = user.id?.let { walletRepository.findByUserId(it) }

        Assertions.assertThat(wallet).isNotNull()
        Assertions.assertThat(wallet!!.user.id).isEqualTo(user.id)
        Assertions.assertThat(wallet.balance).isEqualTo(Money.of(500_000_000L))
        Assertions.assertThat(wallet.address).isEqualTo("Wallet_address_${user.id}")
    }
}