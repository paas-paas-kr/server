package com.document.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Getter
@Configuration
public class NaverOcrConfig {

	@Value("${clova.ocr.api-url}")
	private String apiUrl;

	@Value("${clova.ocr.secret-key}")
	private String secretKey;
}
