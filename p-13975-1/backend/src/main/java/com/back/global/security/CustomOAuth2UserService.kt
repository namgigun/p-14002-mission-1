package com.back.global.security

import com.back.domain.member.member.service.MemberService
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@RequiredArgsConstructor
@Slf4j
class CustomOAuth2UserService : DefaultOAuth2UserService() {
    private val memberService: MemberService? = null

    // 카카오톡 로그인이 성공할 때 마다 이 함수가 실행된다.
    @Transactional
    @Throws(OAuth2AuthenticationException::class)
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)

        var oauthUserId: String? = ""
        val providerTypeCode = userRequest.clientRegistration.registrationId.uppercase(Locale.getDefault())

        var nickname: String? = ""
        var profileImgUrl: String? = ""
        var username = ""

        when (providerTypeCode) {
            "KAKAO" -> {
                val attributes = oAuth2User.attributes
                val attributesProperties = attributes["properties"] as Map<String, Any>?

                oauthUserId = oAuth2User.name
                nickname = attributesProperties!!["nickname"] as String?
                profileImgUrl = attributesProperties["profile_image"] as String?
            }

            "GOOGLE" -> {
                oauthUserId = oAuth2User.name
                nickname = oAuth2User.attributes["name"] as String?
                profileImgUrl = oAuth2User.attributes["picture"] as String?
            }

            "NAVER" -> {
                val attributes = oAuth2User.attributes
                val attributesProperties = attributes["response"] as Map<String, Any>?

                oauthUserId = attributesProperties!!["id"] as String?
                nickname = attributesProperties["nickname"] as String?
                profileImgUrl = attributesProperties["profile_image"] as String?
            }
        }
        username = providerTypeCode + "__%s".formatted(oauthUserId)
        val password = ""

        val member = memberService!!.modifyOrJoin(username, password, nickname!!, profileImgUrl!!).data

        return SecurityUser(
            member!!.id,
            member.username,
            member.password!!,
            member.name,
            member.authorities
        )
    }
}