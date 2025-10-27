package com.common.security;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // 공개 경로 (인증 불필요)
                .requestMatchers(SecurityConstants.PUBLIC_PATHS).permitAll()

                // 관리자 전용 경로 (ADMIN 권한 필요)
                .requestMatchers("/api/*/admin/**").hasRole("ADMIN")  // /api/{service}/admin/** 패턴

                // 나머지 요청은 인증 필요
                .anyRequest().authenticated()
            )
            // 인증/인가 예외 처리 핸들러 등록
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(authenticationEntryPoint)  // 401
                .accessDeniedHandler(accessDeniedHandler)           // 403
            )
            .addFilterBefore(new GatewayAuthenticationFilter(authenticationEntryPoint),
                            UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Gateway 인증 방식을 사용하는 서비스용 더미 UserDetailsService
     * Spring Security가 기본 InMemoryUserDetailsManager를 생성하지 않도록 함
     *
     * 주의: 실제 인증 처리는 GatewayAuthenticationFilter에서 수행됨
     */
    @Bean
    @ConditionalOnMissingBean(
        UserDetailsService.class
    )
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException(
                "Gateway 인증 방식을 사용하는 서비스입니다. UserDetailsService를 직접 사용할 수 없습니다."
            );
        };
    }
}
