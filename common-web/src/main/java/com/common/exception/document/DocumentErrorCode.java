package com.common.exception.document;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DocumentErrorCode {

	DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "문서를 찾을 수 없습니다.", "DOCUMENT_ERROR_404_NOT_FOUND"),
	JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "작업을 찾을 수 없습니다.", "DOCUMENT_ERROR_404_JOB_NOT_FOUND"),
	OCR_EXTRACTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "OCR 추출에 실패했습니다.", "DOCUMENT_ERROR_500_OCR_EXTRACTION_FAILED"),

	INVALID_DOCUMENT_FORMAT(HttpStatus.BAD_REQUEST, "유효하지 않은 문서 형식입니다.", "DOCUMENT_ERROR_400_INVALID_FORMAT"),
	DOCUMENT_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "문서 처리 중 오류가 발생했습니다.",
		"DOCUMENT_ERROR_500_PROCESSING_ERROR"),

	FILE_STORAGE_INITIALIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 스토리지 초기화에 실패했습니다.",
		"DOCUMENT_ERROR_500_FILE_STORAGE_INIT_FAILED");

	private final HttpStatus httpStatus;
	private final String message;
	private final String code;
}


