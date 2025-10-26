package com.document.command;

import org.springframework.web.multipart.MultipartFile;

import com.common.enumtype.Language;

public record UploadImage(
	Long userId,
	MultipartFile[] images,
	Language language
) {
	public static UploadImage of(Long userId, MultipartFile[] images, Language language) {
		return new UploadImage(userId, images, language);
	}
}
