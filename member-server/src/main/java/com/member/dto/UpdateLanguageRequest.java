package com.member.dto;

import com.common.enumtype.Language;
import jakarta.validation.constraints.NotNull;

public record UpdateLanguageRequest(

	@NotNull(message = "선호 언어는 필수입니다")
	Language preferredLanguage
) {
}
