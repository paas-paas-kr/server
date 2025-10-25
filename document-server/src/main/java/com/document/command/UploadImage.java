package com.document.command;

import org.springframework.web.multipart.MultipartFile;

public record UploadImage(
	Long userId,
	MultipartFile[] images,
	String language
) {
	public static UploadImage of(Long userId, MultipartFile[] images, String language) {
		return new UploadImage(userId, images, language);
	}
}
