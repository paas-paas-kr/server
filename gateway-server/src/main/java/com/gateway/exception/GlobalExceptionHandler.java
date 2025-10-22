package com.gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Gateway에서 발생하는 모든 예외를 처리하는 전역 예외 핸들러
 * Spring Cloud Gateway는 WebFlux 기반이므로 ErrorWebExceptionHandler 구현
 */
@Slf4j
@Order(-1)  // 기본 예외 핸들러보다 우선순위를 높게 설정
@Component
@RequiredArgsConstructor
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        log.error("게이트웨이 예외 처리: {}", ex.getMessage(), ex);

        // 예외 타입에 따라 적절한 에러 코드 선택
        GlobalErrorCode errorCode = determineErrorCode(ex);

        // 에러 응답 생성
        ErrorResponse errorResponse = ErrorResponse.from(errorCode);

        // HTTP 응답 설정
        exchange.getResponse().setStatusCode(errorCode.getHttpStatus());
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Error serializing error response", e);
            return exchange.getResponse().setComplete();
        }
    }

    /**
     * 예외 타입에 따라 적절한 에러 코드 결정
     */
    private GlobalErrorCode determineErrorCode(Throwable ex) {
        // ResponseStatusException인 경우 상태 코드에 따라 처리
        if (ex instanceof ResponseStatusException) {
            ResponseStatusException rse = (ResponseStatusException) ex;
            HttpStatus status = HttpStatus.resolve(rse.getStatusCode().value());

            if (status == HttpStatus.NOT_FOUND) {
                return GlobalErrorCode.RESOURCE_NOT_FOUND;
            } else if (status == HttpStatus.UNAUTHORIZED) {
                return GlobalErrorCode.UNAUTHORIZED;
            } else if (status == HttpStatus.FORBIDDEN) {
                return GlobalErrorCode.FORBIDDEN;
            } else if (status == HttpStatus.BAD_REQUEST) {
                return GlobalErrorCode.BAD_REQUEST;
            }
        }

        // 그 외의 경우 500 에러로 처리
        return GlobalErrorCode.INTERNAL_SERVER_ERROR;
    }
}
