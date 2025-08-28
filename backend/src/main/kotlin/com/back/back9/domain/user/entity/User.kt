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
    var wallet: Wallet? = null
) : BaseEntity() {
    enum class UserRole { MEMBER, ADMIN }

    constructor() : this("", "", UserRole.MEMBER, "", UUID.randomUUID().toString(), null)
    constructor(userLoginId: String, username: String, password: String) : this(
        userLoginId, username, UserRole.MEMBER, password, UUID.randomUUID().toString(), null
    )

    fun modifyApiKey(apiKey: String) {
        this.apiKey = apiKey
    }

    fun isAdmin(): Boolean = role == UserRole.ADMIN

    fun getAuthorities(): Collection<GrantedAuthority> {
        val roles = mutableListOf<String>()
        if (isAdmin()) {
            roles.add("ROLE_ADMIN")
        } else {
            roles.add("ROLE_MEMBER")
        }
        return roles.map { SimpleGrantedAuthority(it) }
    }

    data class Builder(
        var userLoginId: String = "",
        var username: String = "",
        var role: UserRole = UserRole.MEMBER,
        var password: String = "",
        var apiKey: String? = null,
        var wallet: Wallet? = null
    ) {
        fun userLoginId(userLoginId: String) = apply { this.userLoginId = userLoginId }
        fun username(username: String) = apply { this.username = username }
        fun role(role: UserRole) = apply { this.role = role }
        fun password(password: String) = apply { this.password = password }
        fun apiKey(apiKey: String) = apply { this.apiKey = apiKey }
        fun wallet(wallet: Wallet?) = apply { this.wallet = wallet }
        fun build() = User(userLoginId, username, role, password, apiKey, wallet)
    }

    companion object {
        @JvmStatic
        fun builder() = Builder()
    }
}