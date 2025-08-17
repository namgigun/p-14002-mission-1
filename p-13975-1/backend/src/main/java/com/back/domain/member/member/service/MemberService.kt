package com.back.domain.member.member.service

import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.repository.MemberRepository
import com.back.global.exception.ServiceException
import com.back.global.rsData.RsData
import lombok.RequiredArgsConstructor
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
@RequiredArgsConstructor
class MemberService(
    private val authTokenService: AuthTokenService,
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder
) {
    fun count(): Long = memberRepository.count()

    fun join(username: String, password: String, nickname: String, profileImgUrl: String? = null): Member {
        if (memberRepository.findByUsername(username) != null) {
            throw ServiceException("409-1", "이미 존재하는 아이디입니다.")
        }

        val encodedPassword = if (password.isNotBlank()) passwordEncoder.encode(password) else null

        val member = Member(username, encodedPassword, nickname, profileImgUrl)
        return memberRepository.save(member)
    }

    fun findByUsername(username: String): Member = memberRepository.findByUsername(username)
        ?: throw UsernameNotFoundException("사용자를 찾을 수 없습니다.")


    fun findByApiKey(apiKey: String): Member = memberRepository.findByApiKey(apiKey)
        ?: throw ServiceException("401-3", "API 키가 유효하지 않습니다.")


    fun genAccessToken(member: Member): String = authTokenService.genAccessToken(member)


    fun payload(accessToken: String): Map<String, Any?>? = authTokenService.payload(accessToken)


    fun findById(id: Int): Member = memberRepository.findById(id)
        .orElseThrow { ServiceException("404", "회원을 찾을 수 없습니다.") }

    fun findAll(): List<Member> = memberRepository.findAll()

    fun checkPassword(member: Member, password: String) {
        if (!passwordEncoder.matches(password, member.password)) {
            throw ServiceException("401-1", "비밀번호가 일치하지 않습니다.")
        }
    }

    fun modifyOrJoin(username: String, password: String, nickname: String, profileImgUrl: String): RsData<Member> {
        val member = findByUsername(username) ?: return RsData(
            "201-1",
            "회원가입이 완료되었습니다.",
            join(username, password, nickname, profileImgUrl)
        )

        modify(member, nickname, profileImgUrl)

        return RsData("200-1", "회원 정보가 수정되었습니다.", member)
    }

    private fun modify(member: Member, nickname: String, profileImgUrl: String?) {
        member.modify(nickname, profileImgUrl)
    }
}