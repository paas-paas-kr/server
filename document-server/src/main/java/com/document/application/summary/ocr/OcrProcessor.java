package com.document.application.summary.ocr;

import java.util.List;

import com.document.domain.Document;

public interface OcrProcessor {

	String extractTextFromDocuments(final List<Document> documents);
}
