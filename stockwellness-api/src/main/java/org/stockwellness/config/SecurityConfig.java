package org.stockwellness.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.stockwellness.application.service.auth.CustomOAuth2UserService;
import org.stockwellness.global.security.handler.OAuth2LoginFailureHandler;
import org.stockwellness.global.security.handler.OAuth2LoginSuccessHandler;
import org.stockwellness.global.security.jwt.JwtAccessDeniedHandler;
import org.stockwellness.global.security.jwt.JwtAuthenticationEntryPoint;
import org.stockwellness.global.security.jwt.JwtAuthenticationFilter;
import org.stockwellness.global.security.jwt.JwtExceptionFilter;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtExceptionFilter jwtExceptionFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

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
                        .requestMatchers("/api/v1/portfolios/**", "/api/v1/watchlist/**", "/api/v1/members/**").authenticated()
                        .anyRequest().permitAll())
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureHandler(oAuth2LoginFailureHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtExceptionFilter, JwtAuthenticationFilter.class)
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(jwtAuthenticationEntryPoint)
                                .accessDeniedHandler(jwtAccessDeniedHandler))
                .build();
    }

    private String[] permitPatterns() {
        return new String[]{
                "/api/v1/auth/**",          // 로그인, 재발급, 더미 로그인 등
                "/api/v1/stocks/**",        // 종목 상세, 랭킹, 검색 — 비로그인 공개 (상세기능은 컨트롤러에서 권한 체크)
                "/api/v1/market/**",        // 시장 지수 — 비로그인 공개
                "/api/v1/sectors/**",       // 섹터 대시보드 — 비로그인 공개 (랭킹, 상세, 비교 분석)
                "/oauth2/**",               // 소셜 로그인 콜백
                "/login/oauth2/**",         // 소셜 로그인 엔드포인트
                "/actuator/**",
                "/docs/**",       // DocsController 및 yaml 파일
                "/swagger-ui/**",          // Swagger UI 경로
                "/v3/api-docs/**",         // (혹시 모를 호환성)
                "/webjars/**",             // [필수] WebJars 리소스 (js, css)
                "/favicon.ico"             // [권장] 파비콘 에러 방지
        };
    }
}
