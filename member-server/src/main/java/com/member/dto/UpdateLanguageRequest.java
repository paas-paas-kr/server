package com.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateLanguageRequest(

	@NotBlank(message = "선호 언어는 필수입니다")
	@Size(min = 2, max = 10, message = "선호 언어는 2자 이상 10자 이하여야 합니다")
	String preferredLanguage
) {
}
