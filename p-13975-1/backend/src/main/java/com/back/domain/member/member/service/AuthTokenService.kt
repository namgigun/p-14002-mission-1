package com.back.domain.member.member.service

import com.back.domain.member.member.entity.Member
import com.back.standard.util.Ut
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class AuthTokenService(
    @Value("\${custom.jwt.secretKey}")
    private val jwtSecretKey: String,
    @Value("\${custom.accessToken.expirationSeconds}")
    private val accessTokenExpirationSeconds: Int
) {
    fun genAccessToken(member: Member): String  =
        Ut.jwt.toString(
            jwtSecretKey,
            accessTokenExpirationSeconds,
            mapOf(
                "id" to member.id,
                "username" to member.username,
                "name" to member.name
            )
        )


    fun payload(accessToken: String): Map<String, Any?>?  =
        Ut.jwt.payload(jwtSecretKey, accessToken)?.let { parsed ->
            mapOf(
                "id" to parsed["id"] as? Int,
                "username" to parsed["username"] as? String,
                "name" to parsed["name"] as? String
            )
        }
}