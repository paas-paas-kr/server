package com.document.application.summary.ocr;

import java.util.List;

import org.springframework.stereotype.Service;

import com.document.application.storage.StorageService;
import com.document.application.summary.ocr.client.ClovaOcrClient;
import com.document.domain.Document;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClovaOcrProcessor implements OcrProcessor {

	private final ClovaOcrClient clovaOcrClient;
	private final StorageService storageService;

	@Override
	public String extractTextFromDocuments(final List<Document> documents) {
		StringBuilder sb = new StringBuilder();

		for (Document doc : documents) {
			// doc.getFilePath()는 이제 NCP 객체 스토리지의 URL
			String fileUrl = storageService.getFileUrl(doc.getFilePath());
			String text = clovaOcrClient.extractText(fileUrl);

			if (text != null && !text.isBlank()) {
				if (documents.size() > 1) {
					sb.append("\n\n=== ").append(doc.getFileName()).append(" ===\n");
				}
				sb.append(text);
			}
		}

		return sb.toString().trim();
	}
}
