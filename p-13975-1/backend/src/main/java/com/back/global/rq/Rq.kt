package com.back.global.rq

import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.service.MemberService
import com.back.global.exception.ServiceException
import com.back.global.security.SecurityUser
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class Rq(
    private val req: HttpServletRequest,
    private val resp: HttpServletResponse,
    private val memberService: MemberService
) {
    val actor: Member
        get() = (SecurityContextHolder.getContext().authentication?.principal as? SecurityUser)
            ?.let { Member(it.id, it.username, it.nickname) }
            ?: throw ServiceException("401-1", "로그인 후 이용할 수 있습니다.")

    val actorFromDb: Member
        get() = actor.let { memberService.findById(it.id) }

    fun getHeader(name: String, defaultValue: String = ""): String =
        req.getHeader(name)?.takeIf { it.isNotBlank() } ?: defaultValue

    fun setHeader(name: String, value: String?) {
        val safeValue = value.orEmpty()
        if (safeValue.isBlank()) {
            req.removeAttribute(name)
        } else {
            resp.setHeader(name, safeValue)
        }
    }

    fun getCookieValue(name: String, defaultValue: String = ""): String =
        req.cookies?.firstOrNull { it.name == name }?.value?.takeIf { it.isNotBlank() }
            ?: defaultValue

    fun setCookie(name: String, value: String?) {
        val safeValue = value.orEmpty()

        val cookie = Cookie(name, safeValue).apply {
            path = "/"
            isHttpOnly = true
            domain = "localhost"
            secure = true
            setAttribute("SameSite", "Strict")
            maxAge = if (safeValue.isBlank()) 0 else 60 * 60 * 24 * 365
        }

        resp.addCookie(cookie)
    }

    fun deleteCookie(name: String) = setCookie(name, null)

    fun sendRedirect(url: String) {
        resp.sendRedirect(url)
    }
}
