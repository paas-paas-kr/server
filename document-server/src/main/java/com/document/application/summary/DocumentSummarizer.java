package com.document.application.summary;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.common.exception.document.DocumentErrorCode;
import com.common.exception.document.DocumentException;
import com.document.application.summary.llm.AiSummaryProcessor;
import com.document.application.summary.ocr.OcrProcessor;
import com.document.domain.Document;
import com.document.domain.SummaryJob;
import com.document.domain.SummaryResult;
import com.document.domain.enumtype.JobStatus;
import com.document.repository.SummaryJobRepository;
import com.document.repository.SummaryResultRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentSummarizer {

	private final SummaryJobRepository summaryJobRepository;
	private final SummaryResultRepository summaryResultRepository;

	private final OcrProcessor ocrProcessor;
	private final AiSummaryProcessor aiSummaryProcessor;
	private final SummaryJobStatusUpdater jobStatusUpdater;

	@Transactional
	public void processSummaryJob(final Long jobId) {
		SummaryJob job = summaryJobRepository.findById(jobId)
			.orElseThrow(() -> {
				log.error("작업을 찾을 수 없습니다 - jobId: {}", jobId);
				return DocumentException.from(DocumentErrorCode.JOB_NOT_FOUND);
			});

		List<Document> documents = job.getDocuments();
		if (documents.isEmpty()) {
			log.error("문서가 없습니다 - jobId: {}", jobId);
			throw DocumentException.from(DocumentErrorCode.DOCUMENT_NOT_FOUND);
		}

		try {
			// 1. OCR 처리
			jobStatusUpdater.updateStatus(job, JobStatus.OCRING);
			String fullText = ocrProcessor.extractTextFromDocuments(documents);
			if (fullText.isEmpty()) {
				log.error("OCR 텍스트 추출 결과가 비어있습니다 - jobId: {}", jobId);
				throw DocumentException.from(DocumentErrorCode.OCR_EXTRACTION_FAILED);
			}
			jobStatusUpdater.updateStatus(job, JobStatus.OCR_COMPLETED);

			// 2. AI 요약 처리
			jobStatusUpdater.updateStatus(job, JobStatus.SUMMARIZING);
			String summary = aiSummaryProcessor.summarizeText(fullText, job.getSummaryLanguage());
			summaryResultRepository.save(SummaryResult.of(job, summary));

			jobStatusUpdater.updateStatus(job, JobStatus.COMPLETED);
		} catch (Exception e) {
			log.error("요약 작업 실패 - jobId: {}, 오류: {}", jobId, e.getMessage(), e);
			throw e;
		}
	}

	@Transactional
	public void handleJobFailure(final Long jobId) {
		log.error("작업 실패 처리 - jobId: {}", jobId);
		summaryJobRepository.findById(jobId).ifPresent(job-> {
			jobStatusUpdater.updateStatus(job, JobStatus.FAILED);
		});
	}
}
