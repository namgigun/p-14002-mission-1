package com.back.global.security

import com.back.domain.member.member.service.MemberService
import com.back.global.rq.Rq
import com.back.standard.extensions.base64Decode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class CustomOAuth2LoginSuccessHandler(
    private val memberService: MemberService,
    private val rq: Rq
) : AuthenticationSuccessHandler {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val actor = rq.actorFromDb
        val accessToken = memberService.genAccessToken(actor)

        rq.setCookie("apiKey", actor.apiKey)
        rq.setCookie("accessToken", accessToken)

        // ✅ state 파라미터에서 redirectUrl 복원
        val redirectUrl = request.getParameter("state")
            ?.base64Decode()
            ?.substringBefore("#")
            ?: "/"

        rq.sendRedirect(redirectUrl)
    }
}