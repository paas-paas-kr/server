package com.common.security;

/**
 * 보안 관련 공통 상수
 * SecurityConfig와 GatewayAuthenticationFilter에서 공유
 */
public class SecurityConstants {

    /**
     * 인증이 필요 없는 공개 경로들
     */
    public static final String[] PUBLIC_PATHS = {
        // Health check
        "/actuator/health/**",
        "/actuator/info/**",

        // Swagger UI
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/member/swagger-ui/**",
        "/member/v3/api-docs/**",
        "/document/swagger-ui/**",
        "/document/v3/api-docs/**",

        // 인증 API (로그인, 회원가입)
        "/api/auth/login",
        "/api/auth/signup"
    };

    /**
     * 경로가 공개 경로인지 확인
     */
    public static boolean isPublicPath(String path) {
        if (path == null) {
            return false;
        }

        for (String pattern : PUBLIC_PATHS) {
            // Exact match
            if (pattern.equals(path)) {
                return true;
            }

            // Wildcard match (/** 패턴)
            if (pattern.endsWith("/**")) {
                String prefix = pattern.substring(0, pattern.length() - 3);
                if (path.startsWith(prefix)) {
                    return true;
                }
            }

            // Single wildcard match (/* 패턴)
            if (pattern.endsWith("/*")) {
                String prefix = pattern.substring(0, pattern.length() - 2);
                if (path.startsWith(prefix) && !path.substring(prefix.length()).contains("/")) {
                    return true;
                }
            }
        }

        return false;
    }

    private SecurityConstants() {
        // Utility class
    }
}
