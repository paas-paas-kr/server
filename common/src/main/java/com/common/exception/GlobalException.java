package com.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class GlobalException extends RuntimeException {

	private final HttpStatus httpStatus;
	private final String code;

	private GlobalException(String message, HttpStatus httpStatus, String code) {
		super(message);
		this.httpStatus = httpStatus;
		this.code = code;
	}

	public static GlobalException from(GlobalErrorCode errorCode) {
		return new GlobalException(errorCode.getMessage(), errorCode.getHttpStatus(), errorCode.getCode());
	}
}
