package com.back.back9.global.security

import com.back.back9.domain.user.entity.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.core.user.OAuth2User

class SecurityUser(
        val user: User
) : UserDetails, OAuth2User {

    override fun getAuthorities(): Collection<GrantedAuthority> =
        listOf<GrantedAuthority>(SimpleGrantedAuthority("ROLE_${user.role.name}"))

    override fun getPassword(): String = user.password

    override fun getUsername(): String = user.userLoginId

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = true

    override fun getAttributes(): Map<String, Any?> = mapOf(
            "id" to user.id,
            "userLoginId" to user.userLoginId,
            "username" to user.username,
            "role" to user.role.name
    )

    override fun getName(): String = user.userLoginId
}