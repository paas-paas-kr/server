package com.document.application.summary;

import org.springframework.stereotype.Component;

import com.document.application.cache.JobStatusCache;
import com.document.domain.SummaryJob;
import com.document.domain.enumtype.JobStatus;
import com.document.repository.SummaryJobRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SummaryJobStatusUpdater {

	private final SummaryJobRepository summaryJobRepository;
	private final JobStatusCache jobStatusCache;

	public void updateStatus(final SummaryJob job, final JobStatus newStatus) {
		job.updateStatus(newStatus);
		summaryJobRepository.save(job);
		jobStatusCache.updateStatus(job.getId(), newStatus);

	}
}
