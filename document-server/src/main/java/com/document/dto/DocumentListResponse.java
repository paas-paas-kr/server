package com.document.dto;

import com.document.domain.enumtype.JobStatus;
import java.time.LocalDateTime;
import java.util.List;

public record DocumentListResponse(
        Long jobId,
        List<String> fileNames,
        JobStatus status,
        String statusMessage,
        LocalDateTime uploadedAt,
        LocalDateTime completedAt
) {

	public static DocumentListResponse of(
		Long jobId,
		List<String> fileNames,
		JobStatus status,
		String statusMessage,
		LocalDateTime uploadedAt,
		LocalDateTime completedAt
	) {
		return new DocumentListResponse(jobId, fileNames, status, statusMessage, uploadedAt, completedAt);
	}
}
