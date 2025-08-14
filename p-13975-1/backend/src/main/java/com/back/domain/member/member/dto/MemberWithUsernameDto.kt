package com.back.domain.member.member.dto

import com.back.domain.member.member.entity.Member
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class MemberWithUsernameDto (
    val id: Int,
    val createDate: LocalDateTime,
    val modifyDate: LocalDateTime,
    val name: String,
    val username: String,
    @JsonProperty("isAdmin")
    val admin: Boolean,
    val profileImageUrl: String
) {
    constructor(member: Member) : this (
        id = member.id,
        createDate = member.createDate,
        modifyDate = member.modifyDate,
        name = member.name,
        username = member.username,
        admin = member.isAdmin,
        profileImageUrl = member.profileImgUrlOrDefault
    )
}
