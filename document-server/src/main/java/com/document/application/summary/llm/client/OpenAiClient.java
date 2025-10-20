package com.document.application.summary.llm.client;

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
	public String summarize(String text, String language) {
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

	private Map<String, Object> buildRequestBody(String text, String language) {
		String systemPrompt = String.format(
			"당신은 문서 요약 전문가입니다. 제공된 텍스트를 간결하고 명확하게 %s로 요약해주세요.",
			language
		);

		String userPrompt = String.format(
			"다음 텍스트를 %s로 요약해주세요:\n\n%s",
			language,
			text
		);

		return Map.of(
			"model", config.getModel(),
			"messages", List.of(
				Map.of("role", "system", "content", systemPrompt),
				Map.of("role", "user", "content", userPrompt)
			),
			"temperature", 0.3,
			"max_tokens", 1000
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
