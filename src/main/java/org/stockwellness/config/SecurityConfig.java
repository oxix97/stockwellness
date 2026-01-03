package org.stockwellness.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.stockwellness.global.security.jwt.JwtAccessDeniedHandler;
import org.stockwellness.global.security.jwt.JwtAuthenticationEntryPoint;
import org.stockwellness.global.security.jwt.JwtAuthenticationFilter;
import org.stockwellness.global.security.jwt.JwtExceptionFilter;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtExceptionFilter jwtExceptionFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            JwtAccessDeniedHandler jwtAccessDeniedHandler
    ) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement((session) -> session
                        .sessionCreationPolicy(STATELESS))
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers(
                                "/api/v1/auth/**",          // 로그인, 재발급, 더미 로그인 등
                                "/oauth2/**",               // 소셜 로그인 콜백 (나중에 추가)
                                "/actuator/**",
                                "/docs/**",       // DocsController 및 yaml 파일
                                "/swagger-ui/**",          // Swagger UI 경로
                                "/v3/api-docs/**",         // (혹시 모를 호환성)
                                "/webjars/**",             // [필수] WebJars 리소스 (js, css)
                                "/favicon.ico"             // [권장] 파비콘 에러 방지
                        ).permitAll()
                        .requestMatchers("/api/v1/admin/**").hasAuthority("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtExceptionFilter, JwtAuthenticationFilter.class)
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(jwtAuthenticationEntryPoint)
                                .accessDeniedHandler(jwtAccessDeniedHandler))
                .build();
    }
}
