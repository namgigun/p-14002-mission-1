package com.back.global.security

import com.back.domain.member.member.service.MemberService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val memberService: MemberService
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val member = memberService.findByUsername(username)

        return SecurityUser(
            member.id,
            member.username,
            "",
            member.name,
            member.authorities
        )
    }
}