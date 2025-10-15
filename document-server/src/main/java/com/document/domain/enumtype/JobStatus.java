package com.document.domain.enumtype;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JobStatus {
	PENDING("작업을 처리하기 위해 대기 중입니다"),
	OCRING("이미지에서 텍스트 추출을 진행 중입니다"),
	OCR_COMPLETED("이미지에서 텍스트 추출이 완료되었습니다"),
	SUMMARIZING("추출된 텍스트를 요약하는 중입니다"),
	COMPLETED("작업이 성공적으로 완료되었습니다"),
	FAILED("작업 처리 중 오류가 발생했습니다");

	private final String message;

}
