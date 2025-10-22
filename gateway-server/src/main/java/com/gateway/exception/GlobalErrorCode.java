package com.gateway.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode {

	// 401 Error Code, 인증 오류
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.", "CLIENT_ERROR_401"),
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 정보입니다.", "CLIENT_ERROR_401_INVALID_CREDENTIALS"),
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.", "CLIENT_ERROR_401_INVALID_TOKEN"),
	TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다.", "CLIENT_ERROR_401_TOKEN_EXPIRED"),

	// 403 Error Code, 권한 오류
	FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.", "CLIENT_ERROR_403"),

	// 400 Error Code, 클라이언트 측 오류
	BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다.", "CLIENT_ERROR_400"),
	INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "유효하지 않은 파라미터입니다.", "CLIENT_ERROR_400_INVALID_PARAMETER"),
	TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.", "CLIENT_ERROR_429_TOO_MANY_REQUESTS"),

	// 404 Error Code, 리소스가 존재하지 않을 때
	RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스가 존재하지 않습니다.", "CLIENT_ERROR_404_RESOURCE_NOT_FOUND"),

	// 500 Error Code, 서버 측 오류
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.", "SERVER_ERROR_500");

	private final HttpStatus httpStatus;
	private final String message;
	private final String code;
}

