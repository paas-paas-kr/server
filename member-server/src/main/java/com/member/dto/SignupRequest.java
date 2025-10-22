package com.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(

	@NotBlank(message = "이메일은 필수입니다")
	@Email(message = "올바른 이메일 형식이 아닙니다")
	String email,

	@NotBlank(message = "비밀번호는 필수입니다")
	@Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다")
	String password,

	@NotBlank(message = "이름은 필수입니다")
	@Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하여야 합니다")
	String name
) {
}
