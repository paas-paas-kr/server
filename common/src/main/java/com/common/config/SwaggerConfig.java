package com.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

	private static final String SECURITY_SCHEME_NAME = "bearerAuth";

	@Value("${gateway.url:http://localhost:8080}")
	private String gatewayUrl;

	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI()
			.servers(List.of(
				new Server()
					.url(gatewayUrl)
					.description("API Gateway Server")
			))
			.info(new Info()
				.title("API Documentation")
				.version("1.0.0")
				.description("Multi-module Spring Boot Application API\n\n"
					+ "## 인증 방법\n"
					+ "1. 우측 상단의 'Authorize' 버튼을 클릭하세요.\n"
					+ "2. JWT 토큰을 입력하세요 (Bearer 접두어 없이 토큰만 입력).\n"
					+ "3. 'Authorize' 버튼을 클릭하여 인증을 완료하세요.\n\n"
					+ "## 역할(Role)\n"
					+ "- **USER**: 일반 사용자 권한\n"
					+ "- **ADMIN**: 관리자 권한 (삭제, 수정 등 특정 기능 접근 가능)\n\n"
					+ "## 주의사항\n"
					+ "**모든 API 요청은 Gateway(" + gatewayUrl + ")를 통해 전송됩니다.**")
				.contact(new Contact()
					.name("API Support")
					.email("support@example.com")))
			.components(new Components()
				.addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
					.name(SECURITY_SCHEME_NAME)
					.type(SecurityScheme.Type.HTTP)
					.scheme("bearer")
					.bearerFormat("JWT")
					.description("JWT 토큰을 입력하세요 (Bearer 접두어 없이)")))
			.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
	}
}

