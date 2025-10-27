package com.common.exception.user;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class UserException extends RuntimeException {

	private final HttpStatus httpStatus;
	private final String code;

	private UserException(String message, HttpStatus httpStatus, String code) {
		super(message);
		this.httpStatus = httpStatus;
		this.code = code;
	}

	public static UserException from(UserErrorCode errorCode) {
		return new UserException(errorCode.getMessage(), errorCode.getHttpStatus(), errorCode.getCode());
	}
}
