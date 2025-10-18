package com.document.application.summary.llm;

import org.springframework.stereotype.Service;

import com.document.application.summary.llm.client.OpenAiClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatGptAiSummaryProcessor implements AiSummaryProcessor {

	private final OpenAiClient openAiClient;

	@Override
	public String summarizeText(String text, String language) {
		return openAiClient.summarize(text, language);
	}
}
