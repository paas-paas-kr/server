package com.document.dto;

public record SummaryResponse(
        Long jobId,
        String statusMessage,
        String summary
) {

	public static SummaryResponse of(Long jobId, String statusMessage, String summary) {
		return new SummaryResponse(jobId, statusMessage, summary);
	}
}
