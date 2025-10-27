package com.common.security;

import com.common.exception.GlobalErrorCode;
import com.common.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증된 사용자가 권한이 없는 리소스에 접근할 때 처리하는 핸들러
 * 403 Forbidden 응답을 공통 ErrorResponse 형식으로 반환
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

	private final ObjectMapper objectMapper;

	@Override
	public void handle(HttpServletRequest request,
		HttpServletResponse response,
		AccessDeniedException accessDeniedException) throws IOException {

		log.error("Access denied: {}", accessDeniedException.getMessage());

		// 403 Forbidden 응답
		GlobalErrorCode errorCode = GlobalErrorCode.FORBIDDEN;
		ErrorResponse errorResponse = ErrorResponse.from(errorCode);

		response.setStatus(errorCode.getHttpStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");

		String json = objectMapper.writeValueAsString(errorResponse);
		response.getWriter().write(json);
	}
}
