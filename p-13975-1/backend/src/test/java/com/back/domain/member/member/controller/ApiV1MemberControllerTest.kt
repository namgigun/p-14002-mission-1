package com.back.domain.member.member.controller

import com.back.domain.member.member.service.MemberService
import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.*
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiV1MemberControllerTest @Autowired constructor(
    private val memberService: MemberService,
    private val mvc: MockMvc
) {
    @Test
    @DisplayName("회원가입")
    fun t1() {
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/members")
                    .content(
                        """
                        {
                            "username": "usernew",
                            "password": "1234",
                            "nickname": "무명"
                        }
                        """.trimIndent()
                    )
                    .contentType(
                        MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8)
                    )
            )
            .andDo(print())

        val member = memberService.findByUsername("usernew")

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("join"))
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("201-1"))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.msg").value("${member.nickname}님 환영합니다. 회원가입이 완료되었습니다.")
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(member.id))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.createDate")
                    .value(startsWith(member.createDate.toString().substring(0, 20)))
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.modifyDate")
                    .value(startsWith(member.modifyDate.toString().substring(0, 20)))
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.name").value(member.nickname))
    }

    @Test
    @DisplayName("로그인")
    fun t2() {
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/members/login")
                    .content(
                        """
                        {
                            "username": "user1",
                            "password": "1234"
                        }
                        """.trimIndent()
                    )
                    .contentType(
                        MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8)
                    )
            )
            .andDo(print())

        val member = memberService.findByUsername("user1")

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("login"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("${member.nickname}님 환영합니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.item").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.item.id").value(member.id))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.item.createDate")
                    .value(startsWith(member.createDate.toString().substring(0, 20)))
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.item.modifyDate")
                    .value(startsWith(member.modifyDate.toString().substring(0, 20)))
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.item.name").value(member.nickname))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.apiKey").value(member.apiKey))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.accessToken").exists())

        resultActions.andExpect { result: MvcResult ->
            val accessTokenCookie = result.response.getCookie("accessToken")!!
            Assertions.assertThat(accessTokenCookie.value).isNotBlank()
            Assertions.assertThat(accessTokenCookie.path).isEqualTo("/")
            Assertions.assertThat(accessTokenCookie.isHttpOnly).isTrue()
            Assertions.assertThat(accessTokenCookie.secure).isTrue()

            val apiKeyCookie = result.response.getCookie("apiKey")!!
            Assertions.assertThat(apiKeyCookie.value).isEqualTo(member.apiKey)
            Assertions.assertThat(apiKeyCookie.path).isEqualTo("/")
            Assertions.assertThat(apiKeyCookie.isHttpOnly).isTrue()
            Assertions.assertThat(apiKeyCookie.secure).isTrue()
        }
    }


    @Test
    @DisplayName("내 정보")
    @WithUserDetails("user1")
    fun t3() {
        val member = memberService.findByUsername("user1")

        val result = mvc.get("/api/v1/members/me")
            .andDo { print() }

        result.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(member.id) }
            jsonPath("$.createDate") { value(startsWith(member.createDate.toString().substring(0, 20))) }
            jsonPath("$.modifyDate") { value(startsWith(member.modifyDate.toString().substring(0, 20))) }
            jsonPath("$.name") { value(member.name) }
            jsonPath("$.username") { value(member.username) }
            jsonPath("$.isAdmin") { value(member.isAdmin) }
        }
    }

    @Test
    @DisplayName("내 정보, with apiKey Cookie")
    @Throws(Exception::class)
    fun t4() {
        val actor = memberService.findByUsername("user1")
        val actorApiKey = actor.apiKey

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/v1/members/me")
                    .cookie(Cookie("apiKey", actorApiKey))
            )
            .andDo(print())

        val member = memberService.findByUsername("user1")

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("me"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.name").value(member.nickname))
    }


    @Test
    @DisplayName("로그아웃")
    @Throws(Exception::class)
    fun t6() {
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.delete("/api/v1/members/logout")
            )
            .andDo(print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("logout"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("로그아웃 되었습니다."))
            .andExpect { result: MvcResult ->
                val apiKeyCookie = result.response.getCookie("apiKey")
                Assertions.assertThat(apiKeyCookie!!.value).isEmpty()
                Assertions.assertThat(apiKeyCookie.maxAge).isEqualTo(0)
                Assertions.assertThat(apiKeyCookie.path).isEqualTo("/")
                Assertions.assertThat(apiKeyCookie.isHttpOnly).isTrue()

                val accessTokenCookie = result.response.getCookie("accessToken")
                Assertions.assertThat(accessTokenCookie!!.value).isEmpty()
                Assertions.assertThat(accessTokenCookie.maxAge).isEqualTo(0)
                Assertions.assertThat(accessTokenCookie.path).isEqualTo("/")
                Assertions.assertThat(accessTokenCookie.isHttpOnly).isTrue()
            }
    }

    @Test
    @DisplayName("엑세스 토큰이 만료되었거나 유효하지 않다면 apiKey를 통해서 재발급")
    fun t7() {
        val actor = memberService.findByUsername("user1")
        val actorApiKey = actor.apiKey

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/v1/members/me")
                    .header("Authorization", "Bearer $actorApiKey wrong-access-token")
            )
            .andDo(print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("me"))
            .andExpect(MockMvcResultMatchers.status().isOk())

        resultActions.andExpect { result: MvcResult ->
            val accessTokenCookie = result.response.getCookie("accessToken")
            Assertions.assertThat(accessTokenCookie!!.value).isNotBlank()
            Assertions.assertThat(accessTokenCookie.path).isEqualTo("/")
            Assertions.assertThat(accessTokenCookie.getAttribute("HttpOnly")).isEqualTo("true")

            val headerAuthorization = result.response.getHeader("Authorization")
            Assertions.assertThat(headerAuthorization).isNotBlank()
            Assertions.assertThat(headerAuthorization).isEqualTo("Bearer user1 " + accessTokenCookie.value)
        }
    }

    @Test
    @DisplayName("Authorization 헤더가 Bearer 형식이 아닐 때 오류")
    fun t8() {
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/v1/members/me")
                    .header("Authorization", "key")
            )
            .andDo(print())

        resultActions
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("401-2"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("Authorization 헤더가 Bearer 형식이 아닙니다."))
    }
}
