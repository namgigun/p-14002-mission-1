package com.back.domain.post.post.controller

import com.back.domain.member.member.service.MemberService
import com.back.domain.post.post.service.PostService
import jakarta.servlet.http.Cookie
import org.hamcrest.Matchers
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiV1PostControllerTest @Autowired constructor(
    private val mvc: MockMvc,
    private val postService: PostService,
    private val memberService: MemberService
) {
    @Test
    @DisplayName("글 쓰기")
    @WithUserDetails("user1")
    fun t1() {
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/posts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                        {
                                            "title": "제목",
                                            "content": "내용"
                                        }
                                        
                                        """.trimIndent()
                    )
            )
            .andDo(MockMvcResultHandlers.print())

        val post = postService.findLatest()

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1PostController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("write"))
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("201-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("${post.id}번 글이 작성되었습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(post.id))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.createDate")
                    .value(Matchers.startsWith(post.createDate.toString().substring(0, 20)))
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.modifyDate")
                    .value(Matchers.startsWith(post.modifyDate.toString().substring(0, 20)))
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.authorId").value(post.author.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.authorName").value(post.author.nickname))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.title").value("제목"))
    }

    @Test
    @DisplayName("글 쓰기, with wrong apiKey, with valid accessToken")
    fun t14() {
        val actor = memberService!!.findByUsername("user1")
        val actorAccessToken = memberService.genAccessToken(actor)

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/posts")
                    .header("Authorization", "Bearer wrong-api-key $actorAccessToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                        {
                                            "title": "제목",
                                            "content": "내용"
                                        }
                                        
                                        """.trimIndent()
                    )
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1PostController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("write"))
            .andExpect(MockMvcResultMatchers.status().isCreated())
    }

    @Test
    @DisplayName("글 쓰기, with wrong apiKey cookie, with valid accessToken cookie")
    fun t15() {
        val actor = memberService.findByUsername("user1")
        val actorAccessToken = memberService.genAccessToken(actor)

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/posts")
                    .cookie(
                        Cookie("apiKey", "wrong-api-key"),
                        Cookie("accessToken", actorAccessToken)
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                        {
                                            "title": "제목",
                                            "content": "내용"
                                        }
                                        
                                        """.trimIndent()
                    )
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1PostController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("write"))
            .andExpect(MockMvcResultMatchers.status().isCreated())
    }

    @Test
    @DisplayName("글 쓰기, without title")
    @WithUserDetails("user1")
    fun t7() {
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/posts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                        {
                                            "title": "",
                                            "content": "내용"
                                        }
                                        
                                        """.trimIndent()
                    )
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1PostController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("write"))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("400-1"))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.msg").value("""
                        title-NotBlank-must not be blank
                        title-Size-size must be between 2 and 100
                        
                        """.trimIndent().trim { it <= ' ' })
            )
    }

    @Test
    @DisplayName("글 쓰기, without content")
    @WithUserDetails("user1")
    fun t8() {
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/posts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                        {
                                            "title": "제목",
                                            "content": ""
                                        }
                                        
                                        """.trimIndent()
                    )
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1PostController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("write"))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("400-1"))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.msg").value("""
                        content-NotBlank-must not be blank
                        content-Size-size must be between 2 and 5000
                        
                        """.trimIndent().trim { it <= ' ' })
            )
    }

    @Test
    @DisplayName("글 쓰기, with wrong json syntax")
    @WithUserDetails("user1")
    fun t9() {
        val wrongJsonBody = """
                {
                    "title": 제목",
                    content": "내용"
                
                """.trimIndent()

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/posts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(wrongJsonBody)
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1PostController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("write"))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("400-1"))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.msg").value("요청 본문이 올바르지 않습니다.".stripIndent().trim { it <= ' ' })
            )
    }

    @Test
    @DisplayName("글 쓰기, without authorization header")
    fun t10() {
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/posts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                        {
                                            "title": "제목",
                                            "content": "내용"
                                        }
                                        
                                        """.trimIndent()
                    )
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("401-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("로그인 후 이용해주세요."))
    }

    @Test
    @DisplayName("글 쓰기, with wrong authorization header")
    fun t11() {
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/posts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer wrong-api-key")
                    .content(
                        """
                                        {
                                            "title": "제목",
                                            "content": "내용"
                                        }
                                        
                                        """.trimIndent()
                    )
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("401-3"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("API 키가 유효하지 않습니다."))
    }


    @Test
    @DisplayName("글 수정")
    @WithUserDetails("user1")
    fun t2() {
        val id = 1

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.put("/api/v1/posts/$id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                        {
                                            "title": "제목 new",
                                            "content": "내용 new"
                                        }
                                        
                                        """.trimIndent()
                    )
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1PostController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("modify"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("${id}번 글이 수정되었습니다."))
    }

    @Test
    @DisplayName("글 수정, without permission")
    fun t12() {
        val id = 1

        val actor = memberService!!.findByUsername("user3")
        val actorApiKey = actor.apiKey

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.put("/api/v1/posts/$id")
                    .header("Authorization", "Bearer $actorApiKey")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                        {
                                            "title": "제목 new",
                                            "content": "내용 new"
                                        }
                                        
                                        """.trimIndent()
                    )
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1PostController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("modify"))
            .andExpect(MockMvcResultMatchers.status().isForbidden())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("403-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("${id}번 글 수정권한이 없습니다."))
    }


    @Test
    @DisplayName("글 삭제")
    @WithUserDetails("user1")
    fun t3() {
        val id = 1

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.delete("/api/v1/posts/$id")
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1PostController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("delete"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("${id}번 글이 삭제되었습니다."))
    }

    @Test
    @DisplayName("글 삭제, without permission")
    fun t13() {
        val id = 1

        val actor = memberService!!.findByUsername("user3")
        val actorApiKey = actor.apiKey

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.delete("/api/v1/posts/$id")
                    .header("Authorization", "Bearer $actorApiKey")
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1PostController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("delete"))
            .andExpect(MockMvcResultMatchers.status().isForbidden())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("403-2"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("${id}번 글 삭제권한이 없습니다."))
    }


    @Test
    @DisplayName("글 단건조회")
    fun t4() {
        val id = 1

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/v1/posts/$id")
            )
            .andDo(MockMvcResultHandlers.print())

        val post = postService.findById(id)

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1PostController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getItem"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(post.id))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.createDate")
                    .value(Matchers.startsWith(post.createDate.toString().substring(0, 20)))
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.modifyDate")
                    .value(Matchers.startsWith(post.modifyDate.toString().substring(0, 20)))
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.authorId").value(post.author.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.authorName").value(post.author.nickname))
            .andExpect(MockMvcResultMatchers.jsonPath("$.title").value(post.title))
            .andExpect(MockMvcResultMatchers.jsonPath("$.content").value(post.content))
    }

    @Test
    @DisplayName("글 단건조회, 404")
    fun t6() {
        val id = Int.MAX_VALUE

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/v1/posts/$id")
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1PostController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getItem"))
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("404-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("해당 데이터가 존재하지 않습니다."))
    }


    @Test
    @DisplayName("글 다건조회")
    @Throws(Exception::class)
    fun t5() {
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/v1/posts")
            )
            .andDo(MockMvcResultHandlers.print())

        val posts = postService.findAll()

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1PostController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getItems"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(posts.size))

        for (i in posts.indices) {
            val post = posts[i]
            resultActions
                .andExpect(MockMvcResultMatchers.jsonPath("$[$i].id").value(post.id))
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$[$i].createDate")
                        .value(Matchers.startsWith(post.createDate.toString().substring(0, 20)))
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$[$i].modifyDate")
                        .value(Matchers.startsWith(post.modifyDate.toString().substring(0, 20)))
                )
                .andExpect(MockMvcResultMatchers.jsonPath("$[$i].authorId").value(post.author.id))
                .andExpect(MockMvcResultMatchers.jsonPath("$[$i].authorName").value(post.author.nickname))
                .andExpect(MockMvcResultMatchers.jsonPath("$[$i].title").value(post.title))
        }
    }
}
