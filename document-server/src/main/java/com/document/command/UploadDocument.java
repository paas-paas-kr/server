package com.document.command;

import org.springframework.web.multipart.MultipartFile;

public record UploadDocument(
	MultipartFile file,
	String language
) {

	public static UploadDocument of(MultipartFile file, String language) {
		return new UploadDocument(file, language);
	}
}
