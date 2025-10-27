package com.document.application.summary.llm.client;

import com.common.enumtype.Language;
import com.document.config.OpenAiConfig;
import com.common.exception.document.DocumentErrorCode;
import com.common.exception.document.DocumentException;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiClient {

	private final OpenAiConfig config;
	private WebClient webClient;


	@PostConstruct
	public void init() {
		this.webClient = WebClient.builder()
			.baseUrl(config.getBaseUrl())
			.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
			.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.build();
	}

	/**
	 * OpenAI API를 호출합니다.
	 */
	public String summarize(String text, Language language) {
		try {
			Map<String, Object> requestBody = buildRequestBody(text, language);

			JsonNode response = webClient.post()
				.uri(config.getChatCompletionsUri())
				.bodyValue(requestBody)
				.retrieve()
				.bodyToMono(JsonNode.class)
				.block();

			if (response == null) {
				throw new Exception();
			}

			String summary = parseSummaryResponse(response);
			return summary;

		} catch (Exception e) {
			throw DocumentException.from(DocumentErrorCode.DOCUMENT_PROCESSING_ERROR);
		}
	}

	private Map<String, Object> buildRequestBody(String text, Language language) {
		String systemPrompt = buildSystemPrompt(language);
		String userPrompt = buildUserPrompt(text, language);

		return Map.of(
			"model", config.getModel(),
			"messages", List.of(
				Map.of("role", "system", "content", systemPrompt),
				Map.of("role", "user", "content", userPrompt)
			),
			"temperature", 0.3,
			"max_tokens", 2500
		);
	}

	private String buildSystemPrompt(Language language) {
		return String.format(
			"""
			당신은 다문화 가정을 위한 문서 분석 전문가입니다.

			**역할:**
			- 다양한 문서(공문서, 안내문, 초대장, 계약서, 고지서, 영수증 등)를 쉽고 명확하게 설명합니다
			- 문서를 보고 해야 할 일들을 정확하게 추출합니다
			- 중요한 날짜와 기한을 강조합니다
			- 주의사항을 명확히 전달합니다

			**응답 형식:**
			- 반드시 %s와 한국어 두 가지 언어로 제공해야 합니다
			- 정돈된 마크다운 형식을 사용합니다
			- 섹션별로 명확하게 구분합니다
			- 쉬운 단어와 짧은 문장을 사용합니다
			""",
			language.getDisplayName()
		);
	}

	private String buildUserPrompt(String text, Language language) {
		String languageInstruction = language == Language.KOREAN
			? "한국어로만 작성해주세요."
			: String.format("%s와 한국어 두 언어로 작성해주세요. 먼저 %s로 작성하고, 그 다음 한국어로 작성해주세요.",
				language.getDisplayName(), language.getDisplayName());

		return String.format(
			"""
			다음 문서를 분석해주세요. %s

			**다음 형식으로 작성해주세요:**

			## 📄 문서 개요
			(이 문서가 무엇인지 2-3문장으로 쉽게 설명)

			## ✅ 해야 할 일
			1. (구체적인 행동 항목)
			2. (구체적인 행동 항목)
			...

			## 📅 중요 날짜 및 기한
			- (날짜): (무엇을 해야 하는지)
			...

			## ⚠️ 주의사항
			- (놓치면 안 되는 중요한 사항)
			...

			## 💡 추가 정보
			(필요한 경우, 문의처나 참고사항)

			---

			**문서 내용:**
			%s
			""",
			languageInstruction,
			text
		);
	}

	private String parseSummaryResponse(JsonNode response) {
		JsonNode contentNode = response.path("choices").path(0).path("message").path("content");
		if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
			throw DocumentException.from(DocumentErrorCode.DOCUMENT_PROCESSING_ERROR);
		}
		return contentNode.asText();
	}
}
