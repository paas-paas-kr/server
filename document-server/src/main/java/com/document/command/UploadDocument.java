package com.document.command;

import org.springframework.web.multipart.MultipartFile;

public record UploadDocument(
	Long userId,
	MultipartFile file,
	String language
) {

	public static UploadDocument of(Long userId, MultipartFile file, String language) {
		return new UploadDocument(userId, file, language);
	}
}
