package com.document.application.command;

import com.common.exception.document.DocumentErrorCode;
import com.common.exception.document.DocumentException;
import com.document.application.storage.StorageService;
import com.document.command.UploadDocument;
import com.document.command.UploadImage;
import com.document.domain.Document;
import com.document.domain.SummaryJob;
import com.document.dto.UploadResponse;
import com.document.application.messaging.SummaryJobPublisher;
import com.document.repository.DocumentRepository;
import com.document.repository.SummaryJobRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentCommandService {

	private final StorageService storageService;
	private final DocumentRepository documentRepository;
	private final SummaryJobRepository summaryJobRepository;

	private final SummaryJobPublisher summaryJobPublisher;

	/**
	 * 1. 파일을 저장 (StorageService)
	 * 2. DB에 Document와 SummaryJob을 동시에 생성
	 * 3. Redis Stream에 작업 메시지 발행
	 * 실패 시 트랜잭션 롤백으로 데이터 일관성 보장
	 */
	@Transactional
	public UploadResponse uploadDocument(final UploadDocument uploadDocument) {
		try {
			// SummaryJob 생성
			SummaryJob job = SummaryJob.of(uploadDocument.language());
			job = summaryJobRepository.save(job);

			// 파일 저장
			String filePath = storageService.store(uploadDocument.file());

			// Document 엔티티 생성 및 Job과 연결
			Document document = Document.of(
				uploadDocument.file().getOriginalFilename(),
				filePath,
				job
			);

			documentRepository.save(document);

			// Redis Stream에 작업 메시지 발행
			summaryJobPublisher.publishJobCreated(job.getId());

			return UploadResponse.of(
				job.getId(),
				job.getStatus()
			);
		} catch (Exception e) {
			throw DocumentException.from(DocumentErrorCode.DOCUMENT_PROCESSING_ERROR);
		}
	}

	/**
	 * 여러 이미지를 업로드하고 하나의 Job으로 처리합니다.
	 * 모든 이미지를 OCR로 텍스트 추출 → 합쳐서 → ChatGPT로 하나의 요약문 생성
	 */
	@Transactional
	public UploadResponse uploadImages(final UploadImage uploadImage) {
		try {
			// SummaryJob 먼저 생성
			SummaryJob job = SummaryJob.of(uploadImage.language());
			job = summaryJobRepository.save(job);

			//  모든 이미지를 저장하고 Document 생성
			List<Document> documents = new ArrayList<>();
			for (MultipartFile image : uploadImage.images()) {
				if (!image.isEmpty()) {
					String filePath = storageService.store(image);

					Document document = Document.of(
						image.getOriginalFilename(),
						filePath,
						job
					);
					documents.add(document);
				}
			}

			// Redis Stream에 작업 메시지 발행
			summaryJobPublisher.publishJobCreated(job.getId());

			return UploadResponse.of(
				job.getId(),
				job.getStatus()
			);

		} catch (Exception e) {
			throw DocumentException.from(DocumentErrorCode.DOCUMENT_PROCESSING_ERROR);
		}
	}
}
