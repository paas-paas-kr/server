package com.document.dto;

import com.document.domain.enumtype.JobStatus;

public record JobStatusResponse(
        Long jobId,
        String statusMessage
) {

	public static JobStatusResponse of(Long jobId, JobStatus status) {
		return new JobStatusResponse(jobId, status.getMessage());
	}
}
