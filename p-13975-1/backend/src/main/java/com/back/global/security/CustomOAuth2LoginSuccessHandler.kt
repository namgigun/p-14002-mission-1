package com.back.global.security

import com.back.domain.member.member.service.MemberService
import com.back.global.rq.Rq
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.*

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
            ?.let { state ->
                val decoded = String(Base64.getUrlDecoder().decode(state), StandardCharsets.UTF_8)
                decoded.substringBefore("#") // '#' 앞 부분이 redirectUrl
            } ?: "/"

        rq.sendRedirect(redirectUrl)
    }
}