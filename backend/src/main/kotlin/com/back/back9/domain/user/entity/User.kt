package com.back.back9.domain.user.entity

import com.back.back9.domain.wallet.entity.Wallet
import com.back.back9.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.util.*

@Entity
@Table(name = "users")
class User(
        @Column(name = "user_login_id", nullable = false, unique = true)
        var userLoginId: String,

        @Column(nullable = false, unique = true)
        var username: String,

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        var role: UserRole = UserRole.MEMBER,

        @Column(nullable = false)
        var password: String,

        @Column(unique = true)
        var apiKey: String? = UUID.randomUUID().toString(),

        @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
        var wallet: Wallet? = null) : BaseEntity() {
        enum class UserRole { MEMBER, ADMIN }

        fun modifyApiKey(apiKey: String) {
            this.apiKey = apiKey
    }

    fun isAdmin(): Boolean = role == UserRole.ADMIN

    fun getAuthorities(): Collection<GrantedAuthority> =
    listOf(SimpleGrantedAuthority(if (isAdmin()) "ROLE_ADMIN" else "ROLE_MEMBER"))
}