package com.common.dto;

public record TokenResponse(
	String accessToken,
	String refreshToken,
	String tokenType,
	Long expiresIn
) {

	public static TokenResponse of(String accessToken, String refreshToken, Long expiresIn) {
		return new TokenResponse(
			accessToken,
			refreshToken,
			"Bearer",
			expiresIn);
	}
}