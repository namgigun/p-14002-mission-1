package com.back.domain.member.member.controller

import com.back.domain.member.member.service.MemberService
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiV1AdmMemberControllerTest @Autowired constructor(
    private val memberService: MemberService,
    private val mvc: MockMvc
) {
    @Test
    @DisplayName("다건조회")
    @WithUserDetails("admin")
    fun t1() {
        val members = memberService.findAll()

        mvc.get("/api/v1/adm/members")
            .andDo { print() }
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(members.size) }
            }.apply {
                members.forEachIndexed { i, member ->
                    andExpect {
                        jsonPath("$[$i].id") { value(member.id) }
                        jsonPath("$[$i].createDate") {
                            value(
                                startsWith(
                                    member.createDate.toString().substring(0, 20)
                                )
                            )
                        }
                        jsonPath("$[$i].modifyDate") {
                            value(
                                startsWith(
                                    member.modifyDate.toString().substring(0, 20)
                                )
                            )
                        }
                        jsonPath("$[$i].name") { value(member.name) }
                        jsonPath("$[$i].username") { value(member.username) }
                        jsonPath("$[$i].isAdmin") { value(member.isAdmin) }
                    }
                }
            }
    }

    @Test
    @DisplayName("다건조회, without permission")
    @WithUserDetails("user1")
    fun t3() {
        mvc.get("/api/v1/adm/members")
            .andDo { print() }
            .andExpect {
                status { isForbidden() }
                jsonPath("$.resultCode") { value("403-1") }
                jsonPath("$.msg") { value("권한이 없습니다.") }
            }
    }

    @Test
    @DisplayName("단건조회")
    @WithUserDetails("admin")
    fun t2() {
        val id = 1
        val member = memberService.findById(id)

        mvc.get("/api/v1/adm/members/$id")
            .andDo { print() }
            .andExpect {
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
    @DisplayName("단건조회, without permission")
    @WithUserDetails("user1")
    fun t4() {
        val id = 1

        mvc.get("/api/v1/adm/members/$id")
            .andDo { print() }
            .andExpect {
                status { isForbidden() }
                jsonPath("$.resultCode") { value("403-1") }
                jsonPath("$.msg") { value("권한이 없습니다.") }
            }
    }
}
