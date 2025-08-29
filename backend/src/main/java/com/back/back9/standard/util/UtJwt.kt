package com.back.back9.standard.util

import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.security.Key
import java.util.Date

object UtJwt {
    fun toString(secret: String, expireSeconds: Int, body: Map<String, Any?>): String {
        val issuedAt = Date()
        val expiration = Date(issuedAt.time + 1000L * expireSeconds)
        val secretKey: Key = Keys.hmacShaKeyFor(secret.toByteArray())

        return Jwts.builder()
            .setClaims(body)
            .setIssuedAt(issuedAt)
            .setExpiration(expiration)
            .signWith(secretKey)
            .compact()
    }

    fun isValid(secret: String, jwtStr: String?): Boolean {
        if (jwtStr.isNullOrBlank()) return false
        val secretKey = Keys.hmacShaKeyFor(secret.toByteArray())
        return try {
            Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(jwtStr)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun payload(secret: String, jwtStr: String?): Map<String, Any?>? {
        if (jwtStr.isNullOrBlank()) return null
        val secretKey = Keys.hmacShaKeyFor(secret.toByteArray())
        return try {
            val claims: Claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(jwtStr)
                .body
            claims
        } catch (_: Exception) {
            null
        }
    }
}