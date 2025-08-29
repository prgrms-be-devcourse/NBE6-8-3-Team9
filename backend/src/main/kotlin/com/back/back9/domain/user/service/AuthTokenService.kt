package com.back.back9.domain.user.service

import com.back.back9.domain.user.entity.User
import com.back.back9.standard.util.UtJwt
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class AuthTokenService {

    @Value("\${custom.jwt.secretKey}")
    private lateinit var jwtSecretKey: String

    @Value("\${custom.jwt.accessToken.expirationSeconds}")
    private var accessTokenExpirationSeconds: Int = 0

    fun genAccessToken(user: User): String {
        val id = user.id
        val userLoginId = user.userLoginId
        val username = user.username
        val role = user.role.name

        return UtJwt.toString( // UtJwt로 변경
            jwtSecretKey,
            accessTokenExpirationSeconds,
            mapOf(
                "id" to id,
                "userLoginId" to userLoginId,
                "username" to username,
                "role" to role
            )
        )
    }

    fun payload(accessToken: String): Map<String, Any?>? {
        val parsedPayload = UtJwt.payload(jwtSecretKey, accessToken) ?: return null // UtJwt로 변경

        val id = (parsedPayload["id"] as? Number)?.toLong()
        val userLoginId = parsedPayload["userLoginId"] as? String
        val username = parsedPayload["username"] as? String
        val role = parsedPayload["role"] as? String

        return mapOf(
            "id" to id,
            "userLoginId" to userLoginId,
            "username" to username,
            "role" to role
        )
    }
}