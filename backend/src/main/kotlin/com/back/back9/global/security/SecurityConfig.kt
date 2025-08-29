package com.back.back9.global.security

import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
class SecurityConfig(
        private val customAuthenticationFilter: CustomAuthenticationFilter,
        private val oAuth2SuccessHandler: OAuth2SuccessHandler,
        private val cookieAuthorizationRequestRepository: HttpCookieOAuth2AuthorizationRequestRepository
) {

    @Bean
    fun securityFilterChain(
            http: HttpSecurity,
            customOAuth2UserService: CustomOAuth2UserService
    ): SecurityFilterChain {
        http
                .csrf { it.disable() }
            .cors(Customizer.withDefaults())
                .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
            it.requestMatchers(
                            "/", "/api/v1/users/login", "/api/v1/users/register",
                            "/api/v1/users/register-admin", "/api/v1/users/logout",
                            "/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html",
                            "/oauth2/**", "/login/oauth2/**", "/login", "/error",
                            "/favicon.ico", "/robots.txt", "/sitemap.xml",
                            "/css/**", "/js/**", "/images/**", "/static/**"
                    ).permitAll()
                    .requestMatchers("/api/v1/adm/**").hasRole("ADMIN")
                    .anyRequest().authenticated()
        }
            .exceptionHandling {
            it.authenticationEntryPoint { _, response, _ ->
                    response.status = HttpServletResponse.SC_UNAUTHORIZED
                response.contentType = "application/json"
                response.writer.write(
                        """{"status":"fail","code":401,"message":"인증이 필요합니다.","result":null}"""
                )
            }
        }
            .oauth2Login {
            it.authorizationEndpoint { authz ->
                    authz.baseUri("/oauth2/authorize")
                            .authorizationRequestRepository(cookieAuthorizationRequestRepository)
            }
            it.redirectionEndpoint { redir ->
                    redir.baseUri("/oauth2/callback/*")
            }
            it.userInfoEndpoint { userInfo ->
                    userInfo.userService(customOAuth2UserService)
            }
            it.successHandler(oAuth2SuccessHandler)
            it.failureHandler { _, res, ex ->
                    ex.printStackTrace()
                val errorMsg = if (ex is OAuth2AuthenticationException) {
                    val err: OAuth2Error = ex.error
                    err.errorCode + (err.description?.let { ": $it" } ?: "")
                } else {
                    ex.message ?: "Unknown error"
                }
                res.status = HttpServletResponse.SC_UNAUTHORIZED
                res.contentType = "application/json"
                res.writer.write(
                        """{"status":"fail","code":401,"message":"${errorMsg.replace("\"", "\\\"")}"}"""
                )
            }
        }
            .logout { it.disable() }
            .addFilterBefore(customAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager =
    config.authenticationManager

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}