package com.common.security;

import com.common.exception.GlobalErrorCode;
import com.common.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증되지 않은 사용자가 보호된 리소스에 접근할 때 처리하는 핸들러
 * 401 Unauthorized 응답을 공통 ErrorResponse 형식으로 반환
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	@Override
	public void commence(HttpServletRequest request,
		HttpServletResponse response,
		AuthenticationException authException) throws IOException {

		log.error("Authentication error: {}", authException.getMessage());

		// 401 Unauthorized 응답
		GlobalErrorCode errorCode = GlobalErrorCode.UNAUTHORIZED;
		ErrorResponse errorResponse = ErrorResponse.from(errorCode);

		response.setStatus(errorCode.getHttpStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE + "; charset=UTF-8");

		String json = objectMapper.writeValueAsString(errorResponse);
		response.getWriter().write(json);
	}
}
