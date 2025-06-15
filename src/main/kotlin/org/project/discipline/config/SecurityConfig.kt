package org.project.discipline.config

import org.project.discipline.domain.user.service.CustomUserDetailsService
import org.project.discipline.security.JwtAuthenticationFilter
import org.project.discipline.security.OAuth2LoginSuccessHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthenticationFilter,
    private val userDetailsService: CustomUserDetailsService,
    private val oauth2LoginSuccessHandler: OAuth2LoginSuccessHandler
) {

    @Bean
    @Order(2)
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain = http
        .csrf { it.disable() }
        .authorizeHttpRequests { auth ->
            auth
                .requestMatchers("/", "/login/**", "/oauth2/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll()
                .requestMatchers("/test/**").permitAll() // 테스트용 API는 인증 불필요
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
        }
        .oauth2Login { oauth2 ->
            oauth2
                .loginPage("/login")
                .successHandler(oauth2LoginSuccessHandler)
        }
        .sessionManagement { session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        }
        .authenticationProvider(authenticationProvider())
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
        .build()

    @Bean
    fun authenticationProvider(): AuthenticationProvider = DaoAuthenticationProvider(userDetailsService)
        .apply { setPasswordEncoder(passwordEncoder()) }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

}