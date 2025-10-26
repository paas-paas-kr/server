package com.common.security;

import com.common.enumtype.Language;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Gateway에서 전달받은 사용자 정보 헤더를 기반으로 인증을 수행하는 필터
 * Gateway에서 이미 JWT 검증을 완료했으므로, 헤더 존재 여부만 확인
 */
@Slf4j
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_EMAIL_HEADER = "X-User-Email";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String USER_LANGUAGE_HEADER = "X-User-Language";

    private final AuthenticationEntryPoint authenticationEntryPoint;

    public GatewayAuthenticationFilter(AuthenticationEntryPoint authenticationEntryPoint) {
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    /**
     * 인증이 필요 없는 경로는 필터를 건너뜀
     * SecurityConstants.PUBLIC_PATHS 사용하여 중복 관리 방지
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return SecurityConstants.isPublicPath(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String userId = request.getHeader(USER_ID_HEADER);
        String userEmail = request.getHeader(USER_EMAIL_HEADER);
        String userRole = request.getHeader(USER_ROLE_HEADER);
        String userLanguage = request.getHeader(USER_LANGUAGE_HEADER);

        try {
            if (userId == null || userEmail == null) {
                throw new MissingAuthenticationHeaderException("필요한 인증 헤더가 누락되었습니다.");
            }

            // Language enum 변환 (기본값: KOREAN)
            Language language = Language.KOREAN;
            if (userLanguage != null && !userLanguage.isEmpty()) {
                try {
                    language = Language.valueOf(userLanguage);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid language header value: {}, using default KOREAN", userLanguage);
                }
            }

            // Gateway에서 전달받은 사용자 정보로 인증 객체 생성
            GatewayUserDetails userDetails = new GatewayUserDetails(
                Long.parseLong(userId),
                userEmail,
                language
            );

            // 역할(Role) 기반 권한 설정
            String role = (userRole != null && !userRole.isEmpty()) ? userRole : "USER";
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);

            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    Collections.singletonList(authority)
                );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (NumberFormatException e) {
            authenticationEntryPoint.commence(request, response,
                new InvalidAuthenticationHeaderException("유효하지 않은 사용자 ID 형식입니다."));
        } catch (AuthenticationException e) {
            authenticationEntryPoint.commence(request, response, e);
        }
    }

    /**
     * 인증 헤더가 누락되었을 때 발생하는 예외
     */
    private static class MissingAuthenticationHeaderException extends AuthenticationException {
        public MissingAuthenticationHeaderException(String msg) {
            super(msg);
        }
    }

    /**
     * 인증 헤더 형식이 잘못되었을 때 발생하는 예외
     */
    private static class InvalidAuthenticationHeaderException extends AuthenticationException {
        public InvalidAuthenticationHeaderException(String msg) {
            super(msg);
        }
    }
}
