package com.common.enumtype;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Language {
	KOREAN("한국어"),
	ENGLISH("English"),
	VIETNAMESE("Tiếng Việt"),
	CHINESE("中文"),
	JAPANESE("日本語");

	private final String displayName;
}
