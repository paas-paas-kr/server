package com.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.exception.ErrorResponse;
import com.gateway.exception.GlobalErrorCode;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final JwtTokenValidator jwtTokenValidator;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtTokenValidator jwtTokenValidator, ObjectMapper objectMapper) {
        super(Config.class);
        this.jwtTokenValidator = jwtTokenValidator;
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // Authorization 헤더 확인
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, GlobalErrorCode.UNAUTHORIZED);
            }

            String authorizationHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
            String token = extractToken(authorizationHeader);

            if (token == null) {
                return onError(exchange, GlobalErrorCode.INVALID_CREDENTIALS);
            }

            // JWT 토큰 검증
            if (!jwtTokenValidator.validateToken(token)) {
                return onError(exchange, GlobalErrorCode.INVALID_TOKEN);
            }

            try {
                // 토큰에서 사용자 정보 추출
                Long userId = jwtTokenValidator.getUserIdFromToken(token);
                String email = jwtTokenValidator.getEmailFromToken(token);
                String role = jwtTokenValidator.getRoleFromToken(token);

                // 요청 헤더에 사용자 정보 추가 (다운스트림 서비스에서 사용)
                ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                        .header("X-User-Id", String.valueOf(userId))
                        .header("X-User-Email", email)
                        .header("X-User-Role", role)
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (Exception e) {
                return onError(exchange, GlobalErrorCode.INTERNAL_SERVER_ERROR);
            }
        };
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출
     */
    private String extractToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }

    /**
     * 에러 응답 반환 (공통 ErrorResponse 형식)
     */
    private Mono<Void> onError(ServerWebExchange exchange, GlobalErrorCode errorCode) {
        log.error("Authentication error: {}", errorCode.getMessage());

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(errorCode.getHttpStatus());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 공통 ErrorResponse 형식으로 응답 생성
        ErrorResponse errorResponse = ErrorResponse.from(errorCode);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Error serializing error response", e);
            return response.setComplete();
        }
    }

    public static class Config {
        // 필요한 설정 값들을 여기에 추가
    }
}
