package com.back.back9.domain.user.controller

import com.back.back9.domain.user.entity.User
import com.back.back9.domain.user.service.UserService
import com.back.back9.domain.user.dto.UserRegisterDto
import jakarta.servlet.http.Cookie
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.*
import org.springframework.test.annotation.Rollback
import org.springframework.transaction.annotation.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue // JUnit의 assertTrue 임포트

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdUserControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var entityManager: EntityManager

    private var apiKeyCookie: Cookie? = null
    private var accessTokenCookie: Cookie? = null

    @BeforeAll
    fun setUpAdmin() {
        if (userService.findByUserLoginId("admin") == null) {
            userService.register(
                UserRegisterDto(
                    "admin",
                    "관리자",
                    "admin1234",
                    "admin1234"
                )
            )
            val admin: User? = userService.findByUserLoginId("admin")
            if (admin == null) throw RuntimeException()
            admin.role = User.UserRole.ADMIN
            userService.save(admin)
        }

        val loginResult = mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "userLoginId": "admin",
                        "password": "admin1234"
                    }
                    """.trimIndent()
                )
        ).andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())

        apiKeyCookie = loginResult.andReturn().response.getCookie("apiKey")
        accessTokenCookie = loginResult.andReturn().response.getCookie("accessToken")
    }

    @Test
    @DisplayName("전체 사용자 조회 - ADMIN 권한")
    fun getAllUsers_withAdmin() {
        val users: List<User?> = userService.findAll()

        val resultActions = mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/adm/users")
                .cookie(apiKeyCookie, accessTokenCookie)
        ).andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())

        resultActions
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk)
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("\$.result.length()").value(users.size))

        for (i in users.indices) {
            val user = users[i]
            resultActions
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("\$.result[$i].id").value(user?.id))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("\$.result[$i].username").value(user?.username))
        }
    }

    @Test
    @DisplayName("단일 사용자 조회 - ADMIN 권한")
    fun getUserById_withAdmin() {
        val dto = UserRegisterDto("user1", "유저1", "password", "password")
        userService.register(dto)
        val user: User? = userService.findByUserLoginId("user1")
        if (user == null) throw RuntimeException()
        val id: Long? = user.id

        val resultActions = mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/adm/users/{id}", id)
                .cookie(apiKeyCookie, accessTokenCookie)
        ).andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())

        resultActions
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk)
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("\$.result.id").value(user.id))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("\$.result.username").value(user.username))
    }

    @Test
    @DisplayName("username으로 검색 - ADMIN 권한")
    fun searchUserByUsername_withAdmin() {
        val keyword = "user"
        val users: List<User?> = userService.searchByUsername(keyword)

        val resultActions = mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/adm/users/search")
                .param("keyword", keyword)
                .cookie(apiKeyCookie, accessTokenCookie)
        ).andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())

        resultActions
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk)
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("\$.result.length()").value(users.size))

        for (i in users.indices) {
            val user = users[i]
            resultActions
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("\$.result[$i].id").value(user?.id))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("\$.result[$i].username").value(user?.username))
        }
    }

    @Test
    @DisplayName("userLoginId로 사용자 삭제 - ADMIN 권한")
    @Transactional
    @Rollback(false)
    fun deleteUserByLoginId_withAdmin() {
        val loginId = "deleteTestUser"
        userService.register(
            UserRegisterDto(
                loginId,
                "삭제테스트",
                "test1234",
                "test1234"
            )
        )

        entityManager.flush()

        val resultActions = mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/adm/users/loginId/{userLoginId}", loginId)
                .cookie(apiKeyCookie, accessTokenCookie)
        ).andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())

        resultActions
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk)
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("\$.code").value("200"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("\$.message").value("사용자가 성공적으로 삭제되었습니다."))

        entityManager.flush()
        entityManager.clear()

        val count = entityManager.createQuery(
            "SELECT COUNT(u) FROM User u WHERE u.userLoginId = :loginId", java.lang.Long::class.java
        )
            .setParameter("loginId", loginId)
            .singleResult

        assertTrue(count.toLong() == 0L) // 타입 변환 후 비교
    }
}