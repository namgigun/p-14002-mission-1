package com.back.global.security

import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.service.MemberService
import com.back.global.exception.ServiceException
import com.back.global.rq.Rq
import com.back.standard.util.Ut
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import lombok.RequiredArgsConstructor
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

@Component
@RequiredArgsConstructor
class CustomAuthenticationFilter(
    private val memberService: MemberService,
    private val rq: Rq
) : OncePerRequestFilter() {
    @Throws(ServletException::class, java.io.IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        logger.debug("Processing request for " + request.requestURI)

        try {
            work(request, response, filterChain)
        } catch (e: ServiceException) {
            val rsData = e.rsData
            response.contentType = "application/json;charset=UTF-8"
            response.status = rsData.statusCode
            response.writer.write(
                Ut.json.toString(rsData)
            )
        }
    }

    @Throws(ServletException::class, IOException::class)
    private fun work(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        // API 요청이 아니라면 패스
        if (!request.requestURI.startsWith("/api/")) {
            filterChain.doFilter(request, response)
            return
        }

        // 2. 인증 불필요한 엔드포인트
        val publicUris = setOf(
            "/api/v1/members/login",
            "/api/v1/members/logout",
            "/api/v1/members/join"
        )
        if (request.requestURI in publicUris) {
            filterChain.doFilter(request, response)
            return
        }

        val headerAuthorization = rq.getHeader("Authorization", "")

        val (apiKey, accessToken) = if (headerAuthorization.isNotBlank()) {
            if (!headerAuthorization.startsWith("Bearer ")) {
                throw ServiceException("401-2", "Authorization 헤더가 Bearer 형식이 아닙니다.")
            }
            val parts = headerAuthorization.split(" ", limit = 3)
            parts.getOrNull(1).orEmpty() to parts.getOrNull(2).orEmpty()
        } else {
            rq.getCookieValue("apiKey", "") to rq.getCookieValue("accessToken", "")
        }

        logger.debug("apiKey : $apiKey")
        logger.debug("accessToken : $accessToken")

        val isApiKeyExists = apiKey.isNotBlank()
        val isAccessTokenExists = accessToken.isNotBlank()

        if (!isApiKeyExists && !isAccessTokenExists) {
            filterChain.doFilter(request, response)
            return
        }

        // 4. AccessToken 검증
        var member: Member? = null
        var isAccessTokenValid = false

        if (isAccessTokenExists) {
            memberService.payload(accessToken)?.let { payload ->
                val id = payload["id"] as Int
                val username = payload["username"] as? String ?: throw ServiceException("401-3", "잘못된 토큰입니다.")
                val name = payload["name"] as? String ?: throw ServiceException("401-4", "잘못된 토큰입니다.")
                member = Member(id, username, name)
                isAccessTokenValid = true
            }
        }

        // 5. API Key로 조회
        if (member == null && isApiKeyExists) {
            member = memberService.findByApiKey(apiKey)
        }

        // 6. AccessToken 갱신
        if (isAccessTokenExists && !isAccessTokenValid && member != null) {
            val newToken = memberService.genAccessToken(member!!)
            rq.setCookie("accessToken", newToken)
            rq.setHeader("Authorization", "Bearer $apiKey $newToken")
        }

        val actor = member ?: throw ServiceException("401-1", "인증된 사용자를 찾을 수 없습니다.")

        // 7. 인증 정보 등록
        val user: UserDetails = SecurityUser(
            actor.id,
            actor.username,
            "",
            actor.name,
            actor.authorities
        )

        val authentication = UsernamePasswordAuthenticationToken(user, user.password, user.authorities)
        SecurityContextHolder.getContext().authentication = authentication

        filterChain.doFilter(request, response)
    }
}
