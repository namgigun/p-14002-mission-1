package com.back.domain.member.member.service

import com.back.standard.util.Ut.jwt.isValid
import com.back.standard.util.Ut.jwt.payload
import com.back.standard.util.Ut.jwt.toString
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.util.*

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthTokenServiceTest @Autowired constructor(
    private val memberService: MemberService,
    private val authTokenService: AuthTokenService
) {
    @Value("\${custom.jwt.secretKey}")
    private lateinit var jwtSecretKey: String

    @Value("\${custom.accessToken.expirationSeconds}")
    private var accessTokenExpirationSeconds: Int = 0

    @Test
    @DisplayName("authTokenService 서비스가 존재한다.")
    fun t1() {
        assertThat(authTokenService).isNotNull
    }

    @Test
    @DisplayName("jjwt 최신 방식으로 JWT 생성, {name=\"Paul\", age=23}")
    fun t2() {
        val expireMillis = 1000L * accessTokenExpirationSeconds
        val secretKey = Keys.hmacShaKeyFor(jwtSecretKey.toByteArray(StandardCharsets.UTF_8))

        val issuedAt = Date()
        val expiration = Date(issuedAt.time + expireMillis)

        val payload = mapOf(
            "name" to "Paul",
            "age" to 23
        )

        val jwt = Jwts.builder()
            .claims(payload)
            .issuedAt(issuedAt)
            .expiration(expiration)
            .signWith(secretKey)
            .compact()

        assertThat(jwt).isNotBlank()
        println("jwt = $jwt")

        val parsedPayload = Jwts
            .parser()
            .verifyWith(secretKey)
            .build()
            .parse(jwt)
            .payload as Map<String, Any>

        assertThat(parsedPayload).containsAllEntriesOf(payload)
    }

    @Test
    @DisplayName("Ut.jwt.toString 를 통해서 JWT 생성, {name=\"Paul\", age=23}")
    fun t3() {
        val payload = mapOf("name" to "Paul", "age" to 23)

        val jwt = toString(jwtSecretKey, accessTokenExpirationSeconds, payload)

        assertThat(jwt).isNotBlank()
        assertThat(isValid(jwtSecretKey, jwt)).isTrue()

        val parsedPayload = payload(jwtSecretKey, jwt)
        assertThat(parsedPayload).containsAllEntriesOf(payload)
    }

    @Test
    @DisplayName("authTokenService.genAccessToken(member);")
    fun t4() {
        val member = memberService.findByUsername("user1")
        val accessToken = authTokenService.genAccessToken(member)

        assertThat(accessToken).isNotBlank()
        println("accessToken = $accessToken")

        val parsedPayload = authTokenService.payload(accessToken)
        assertThat(parsedPayload).containsAllEntriesOf(
            mapOf(
                "id" to member.id,
                "username" to member.username,
                "name" to member.name
            )
        )
    }
}