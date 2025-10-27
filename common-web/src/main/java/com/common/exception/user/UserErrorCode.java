package com.common.exception.user;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode {

	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.", "USER_ERROR_404_NOT_FOUND"),
	DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "이미 존재하는 이메일입니다.", "USER_ERROR_400_DUPLICATE_EMAIL"),
	INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 올바르지 않습니다.", "USER_ERROR_401_INVALID_PASSWORD"),
	INVALID_ACCOUNT_STATUS(HttpStatus.FORBIDDEN, "유효하지 않은 계정 상태입니다.", "USER_ERROR_403_INVALID_ACCOUNT_STATUS");

	private final HttpStatus httpStatus;
	private final String message;
	private final String code;
}


