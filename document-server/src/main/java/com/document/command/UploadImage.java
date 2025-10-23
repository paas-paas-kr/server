package com.document.command;

import org.springframework.web.multipart.MultipartFile;

public record UploadImage(
	MultipartFile[] images,
	String language
) {
	public static UploadImage of(MultipartFile[] images, String language) {
		return new UploadImage(images, language);
	}
}
