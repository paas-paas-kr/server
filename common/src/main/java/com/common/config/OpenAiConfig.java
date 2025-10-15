package com.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Getter
@Configuration
public class OpenAiConfig {

    @Value("${openai.api-key}")
    private String apiKey;

    /** 사용할 모델명 (기본값: gpt-4o-mini) */
    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    /** OpenAI API Base URL */
    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    /** Chat Completion 요청 URI */
    @Value("${openai.uri.chat-completions:/chat/completions}")
    private String chatCompletionsUri;
}
