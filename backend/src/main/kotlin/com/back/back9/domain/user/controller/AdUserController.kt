package com.back.back9.domain.user.controller

import com.back.back9.domain.user.dto.UserWithUsernameDto
import com.back.back9.domain.user.service.UserService
import com.back.back9.global.error.ErrorCode
import com.back.back9.global.error.ErrorException
import com.back.back9.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/v1/adm/users")
@Tag(name = "AdUserController", description = "관리자용 사용자 API")
@SecurityRequirement(name = "bearerAuth")
class AdUserController(
        private val userService: UserService
) {

    @GetMapping
    @Operation(summary = "전체 사용자 조회")
    fun getUsers(): RsData<List<UserWithUsernameDto>> {
        val dtos = userService.findAll()
                .map(::UserWithUsernameDto)

        return RsData(
                "200",
                "사용자 정보를 성공적으로 조회했습니다.",
                dtos
        )
    }

    @GetMapping("/{id}")
    @Operation(summary = "id로 사용자 조회")
    fun getUser(@PathVariable id: Long): RsData<UserWithUsernameDto> {
        val user = userService.findById(id)
                ?: throw ErrorException(ErrorCode.USER_NOT_FOUND, id)

        return RsData(
                "200",
                "사용자 정보를 성공적으로 조회했습니다.",
                UserWithUsernameDto(user)
        )
    }

    @GetMapping("/search")
    @Operation(summary = "username으로 사용자 검색")
    fun searchUsersByUsername(@RequestParam keyword: String): RsData<List<UserWithUsernameDto>> {
        val dtos = userService.searchByUsername(keyword)
                .map(::UserWithUsernameDto)

        return RsData(
                "200",
                "사용자 검색 결과입니다.",
                dtos
        )
    }

    @DeleteMapping("/loginId/{userLoginId}")
    @Operation(summary = "userLoginId로 사용자 삭제")
    fun deleteUserByLoginId(@PathVariable userLoginId: String): RsData<Void> {
        userService.deleteByUserLoginId(userLoginId)
        return RsData("200", "사용자가 성공적으로 삭제되었습니다.", null)
    }
}