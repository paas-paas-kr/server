package com.document.dto;

import com.document.domain.enumtype.JobStatus;

public record UploadResponse(
        Long jobId,
        JobStatus status,
        String statusMessage
) {

	public static UploadResponse of(Long jobId, JobStatus status) {
		return new UploadResponse(jobId, status, status.getMessage());
	}
}
