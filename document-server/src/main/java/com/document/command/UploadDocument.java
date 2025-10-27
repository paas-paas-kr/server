package com.document.command;

import org.springframework.web.multipart.MultipartFile;

import com.common.enumtype.Language;

public record UploadDocument(
	Long userId,
	MultipartFile file,
	Language language
) {

	public static UploadDocument of(Long userId, MultipartFile file, Language language) {
		return new UploadDocument(userId, file, language);
	}
}
