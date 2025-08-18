package com.back.domain.post.postComment.controller

import com.back.domain.member.member.service.MemberService
import com.back.domain.post.post.service.PostService
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
class ApiV1PostCommentControllerTest @Autowired constructor(
    private val mvc: MockMvc,
    private val postService: PostService,
    private val memberService: MemberService
) {
    @Test
    @DisplayName("댓글 단건조회")
    fun t1() {
        val postId = 1
        val id = 1

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/v1/posts/${postId}/comments/${id}")
            )
            .andDo(MockMvcResultHandlers.print())

        val post = postService.findById(postId)
        val postComment = post.findCommentById(id)

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1PostCommentController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getItem"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(postComment.id))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.createDate")
                    .value(Matchers.startsWith(postComment.createDate.toString().substring(0, 20)))
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.modifyDate")
                    .value(Matchers.startsWith(postComment.modifyDate.toString().substring(0, 20)))
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.authorId").value(postComment.author.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.authorName").value(postComment.author.name))
            .andExpect(MockMvcResultMatchers.jsonPath("$.postId").value(postComment.post.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.content").value(postComment.content))
    }

    @Test
    @DisplayName("댓글 다건조회")
    fun t2() {
        val postId = 1

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/v1/posts/${postId}/comments")
            )
            .andDo(MockMvcResultHandlers.print())

        val post = postService.findById(postId)
        val comments = post.comments

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1PostCommentController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getItems"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(comments.size))

        for (i in comments.indices) {
            val postComment = comments[i]

            resultActions
                .andExpect(MockMvcResultMatchers.jsonPath("$[$i].id").value(postComment.id))
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$[$i].createDate")
                        .value(Matchers.startsWith(postComment.createDate.toString().substring(0, 20)))
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$[$i].modifyDate")
                        .value(Matchers.startsWith(postComment.modifyDate.toString().substring(0, 20)))
                )
                .andExpect(MockMvcResultMatchers.jsonPath("$[$i].authorId").value(postComment.author.id))
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$[$i].authorName").value(postComment.author.name)
                )
                .andExpect(MockMvcResultMatchers.jsonPath("$[$i].postId").value(postComment.post.id))
                .andExpect(MockMvcResultMatchers.jsonPath("$[$i].content").value(postComment.content))
        }
    }

    @Test
    @DisplayName("댓글 삭제")
    @WithUserDetails("user1")
    fun t3() {
        val postId = 1
        val id = 1

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.delete("/api/v1/posts/${postId}/comments/${id}")
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1PostCommentController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("delete"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("${id}번 댓글이 삭제되었습니다."))
    }

    @Test
    @DisplayName("댓글 삭제, without permission")
    @Throws(Exception::class)
    fun t7() {
        val postId = 1
        val id = 1

        val actor = memberService.findByUsername("user3")
        val actorApiKey = actor.apiKey

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.delete("/api/v1/posts/${postId}/comments/${id}")
                    .header("Authorization", "Bearer $actorApiKey")
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1PostCommentController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("delete"))
            .andExpect(MockMvcResultMatchers.status().isForbidden())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("403-2"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("${id}번 댓글 삭제권한이 없습니다."))
    }


    @Test
    @DisplayName("댓글 수정")
    @WithUserDetails("user1")
    fun t4() {
        val postId = 1
        val id = 1

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.put("/api/v1/posts/${postId}/comments/${id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                        {
                                            "content": "내용 new"
                                        }
                                        
                                        """.trimIndent()
                    )
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1PostCommentController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("modify"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("${id}번 댓글이 수정되었습니다."))
    }

    @Test
    @DisplayName("댓글 수정, without permission")
    @Throws(Exception::class)
    fun t6() {
        val postId = 1
        val id = 1

        val actor = memberService.findByUsername("user3")
        val actorApiKey = actor.apiKey

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.put("/api/v1/posts/${postId}/comments/${id}")
                    .header("Authorization", "Bearer $actorApiKey")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                        {
                                            "content": "내용 new"
                                        }
                                        
                                        """.trimIndent()
                    )
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1PostCommentController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("modify"))
            .andExpect(MockMvcResultMatchers.status().isForbidden())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("403-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("${id}번 댓글 수정권한이 없습니다."))
    }


    @Test
    @DisplayName("댓글 작성")
    @WithUserDetails("user1")
    fun t5() {
        val postId = 1

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/posts/${postId}/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                        {
                                            "content": "내용"
                                        }
                                        
                                        """.trimIndent()
                    )
            )
            .andDo(MockMvcResultHandlers.print())

        val post = postService.findById(postId)

        val postComment = post.comments.last()

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1PostCommentController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("write"))
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("201-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("${postComment.id}번 댓글이 작성되었습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(postComment.id))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.createDate")
                    .value(Matchers.startsWith(postComment.createDate.toString().substring(0, 20)))
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.modifyDate")
                    .value(Matchers.startsWith(postComment.modifyDate.toString().substring(0, 20)))
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.authorId").value(postComment.author.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.authorName").value(postComment.author.name))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.postId").value(postComment.post.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content").value("내용"))
    }
}
