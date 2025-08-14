package com.back.global.app

import com.back.standard.util.Ut
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
class AppConfig(
    private val environment: Environment,
    private val objectMapper: ObjectMapper
) {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @PostConstruct
    fun init() {
        // Ut 객체에 ObjectMapper 할당
        Ut.json.objectMapper = objectMapper
    }

    companion object {
        lateinit var environmentRef: Environment
            private set
        val isDev: Boolean
            get() = environmentRef.matchesProfiles("dev")

        val isTest: Boolean
            get() = environmentRef.matchesProfiles("test")

        val isProd: Boolean
            get() = environmentRef.matchesProfiles("prod")

        val isNotProd: Boolean
            get() = !isProd


    }

    init {
        // companion object에 environment 할당
        environmentRef = environment
    }
}
