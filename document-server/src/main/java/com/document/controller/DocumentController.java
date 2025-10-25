package com.document.controller;

import com.common.response.DataResponse;
import com.common.security.GatewayUserDetails;
import com.document.command.UploadDocument;
import com.document.command.UploadImage;
import com.document.dto.DocumentListResponse;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Document", description = "문서 요약 API")
public class DocumentController {

	private final DocumentCommandService uploadService;
	private final DocumentQueryService documentQueryService;

	@GetMapping
	@Operation(summary = "문서 목록 조회", description = "사용자 ID 기반으로 문서 목록을 조회합니다. (JWT 필요)")
	public ResponseEntity<DataResponse<List<DocumentListResponse>>> getDocumentList(
			@AuthenticationPrincipal GatewayUserDetails userDetails
	) {
		List<DocumentListResponse> documents = documentQueryService.getAllDocuments(userDetails.getUserId());
		return ResponseEntity.ok(DataResponse.from(documents));
	}

	@PostMapping(value = "/upload/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "문서 업로드", description = "단일 문서를 업로드하고 요약 작업을 시작합니다. (JWT 필요)")
	public ResponseEntity<DataResponse<UploadResponse>> uploadDocument(
			@AuthenticationPrincipal GatewayUserDetails userDetails,
			@Valid @RequestParam(value = "file", required = true) MultipartFile file,
			@Valid @RequestParam(value = "language", required = false, defaultValue = "ko") String language
	) {
		UploadResponse uploadResponse = uploadService.uploadDocument(
			UploadDocument.of(userDetails.getUserId(), file, language)
		);
		return ResponseEntity.ok(DataResponse.from(uploadResponse));
	}

	@PostMapping(value = "/upload/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "이미지 업로드", description = "여러 이미지를 업로드하고 하나의 요약 작업을 시작합니다. 모든 이미지의 텍스트를 추출하여 하나의 요약문을 생성합니다. (JWT 필요)")
	public ResponseEntity<DataResponse<UploadResponse>> uploadImages(
			@AuthenticationPrincipal GatewayUserDetails userDetails,
			@Valid @RequestPart(value = "images", required = true) MultipartFile[] images,
			@Valid @RequestParam(value = "language", required = false, defaultValue = "ko") String language
	) {
		UploadResponse uploadResponse = uploadService.uploadImages(
			UploadImage.of(userDetails.getUserId(), images, language)
		);
		return ResponseEntity.ok(DataResponse.from(uploadResponse));
	}

	@GetMapping("/{jobId}/status")
	@Operation(summary = "작업 상태 조회", description = "요약 작업의 진행 상태를 조회합니다. (JWT 필요)")
	public DataResponse<JobStatusResponse> getJobStatus(
			@AuthenticationPrincipal GatewayUserDetails userDetails,
			@PathVariable Long jobId
	) {
		// 필요시 userDetails.getUserId()로 작업 소유자 확인 가능
		JobStatusResponse response = documentQueryService.getJobStatus(jobId);
		return DataResponse.from(response);
	}

	@GetMapping("/{jobId}/summary")
	@Operation(summary = "요약 결과 조회", description = "완료된 요약 결과를 조회합니다. (JWT 필요)")
	public DataResponse<SummaryResponse> getSummary(
			@AuthenticationPrincipal GatewayUserDetails userDetails,
			@PathVariable Long jobId
	) {
		// 필요시 userDetails.getUserId()로 작업 소유자 확인 가능
		SummaryResponse response = documentQueryService.getSummary(jobId);
		return DataResponse.from(response);
	}

	/**
	 * 권한 기반 접근 제어 예시
	 * ADMIN 역할만 문서 삭제 가능 (SecurityConfig의 URL 패턴으로 제어)
	 */
	@DeleteMapping("/{jobId}")
	@Operation(summary = "문서 삭제 (관리자 전용)", description = "요약 작업과 관련 문서를 삭제합니다. ADMIN 권한이 필요합니다.")
	public ResponseEntity<DataResponse<String>> deleteDocument(
			@AuthenticationPrincipal GatewayUserDetails userDetails,
			@PathVariable Long jobId
	) {
		// 실제 삭제 로직은 구현되지 않음 (예시용)
		// documentCommandService.deleteDocument(jobId);
		return ResponseEntity.ok(DataResponse.from("문서 삭제 성공 (관리자 전용 기능)"));
	}

	/**
	 * 본인의 문서만 수정 가능한 예시
	 * 관리자이거나 문서 소유자인 경우에만 접근 가능 (SecurityConfig의 URL 패턴으로 제어)
	 */
	@PutMapping("/{jobId}")
	@Operation(summary = "문서 수정 (본인 또는 관리자)", description = "문서를 수정합니다. 본인의 문서이거나 ADMIN 권한이 필요합니다.")
	public ResponseEntity<DataResponse<String>> updateDocument(
			@AuthenticationPrincipal GatewayUserDetails userDetails,
			@PathVariable Long jobId
	) {
		// 실제 수정 로직은 구현되지 않음 (예시용)
		// documentCommandService.updateDocument(jobId);
		return ResponseEntity.ok(DataResponse.from("문서 수정 성공"));
	}
}