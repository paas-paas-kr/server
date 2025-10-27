package com.common.exception.document;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class DocumentException extends RuntimeException {

	private final HttpStatus httpStatus;
	private final String code;

	private DocumentException(String message, HttpStatus httpStatus, String code) {
		super(message);
		this.httpStatus = httpStatus;
		this.code = code;
	}

	public static DocumentException from(DocumentErrorCode errorCode) {
		return new DocumentException(errorCode.getMessage(), errorCode.getHttpStatus(), errorCode.getCode());
	}
}
