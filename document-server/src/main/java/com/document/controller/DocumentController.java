package com.document.controller;

import com.common.response.DataResponse;
import com.document.command.UploadDocument;
import com.document.command.UploadImage;
import com.document.dto.JobStatusResponse;
import com.document.dto.SummaryResponse;
import com.document.dto.UploadResponse;
import com.document.application.query.DocumentQueryService;
import com.document.application.command.DocumentCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Document", description = "문서 요약 API")
public class DocumentController {

	private final DocumentCommandService uploadService;
	private final DocumentQueryService documentQueryService;

	@PostMapping(value = "/upload/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "문서 업로드", description = "단일 문서를 업로드하고 요약 작업을 시작합니다.")
	public ResponseEntity<DataResponse<UploadResponse>> uploadDocument(
			@Valid @RequestParam(value = "file", required = true) MultipartFile file,
			@Valid @RequestParam(value = "language", required = false, defaultValue = "ko") String language
	) {
		UploadResponse uploadResponse = uploadService.uploadDocument(UploadDocument.of(file, language));
		return ResponseEntity.ok(DataResponse.from(uploadResponse));
	}

	@PostMapping(value = "/upload/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "이미지 업로드", description = "여러 이미지를 업로드하고 하나의 요약 작업을 시작합니다. 모든 이미지의 텍스트를 추출하여 하나의 요약문을 생성합니다.")
	public ResponseEntity<DataResponse<UploadResponse>> uploadImages(
			@Valid @RequestPart(value = "images", required = true) MultipartFile[] images,
			@Valid @RequestParam(value = "language", required = false, defaultValue = "ko") String language
	) {
		UploadResponse uploadResponse = uploadService.uploadImages(UploadImage.of(images, language));
		return ResponseEntity.ok(DataResponse.from(uploadResponse));
	}

	@GetMapping("/{jobId}/status")
	@Operation(summary = "작업 상태 조회", description = "요약 작업의 진행 상태를 조회합니다.")
	public DataResponse<JobStatusResponse> getJobStatus(
			@PathVariable Long jobId
	) {
		JobStatusResponse response = documentQueryService.getJobStatus(jobId);
		return DataResponse.from(response);
	}

	@GetMapping("/{jobId}/summary")
	@Operation(summary = "요약 결과 조회", description = "완료된 요약 결과를 조회합니다.")
	public DataResponse<SummaryResponse> getSummary(
			@PathVariable Long jobId
	) {
		SummaryResponse response = documentQueryService.getSummary(jobId);
		return DataResponse.from(response);
	}
}