package com.document.application.query;

import com.common.exception.document.DocumentErrorCode;
import com.common.exception.document.DocumentException;
import com.document.domain.enumtype.JobStatus;
import com.document.domain.SummaryJob;
import com.document.domain.SummaryResult;
import com.document.dto.JobStatusResponse;
import com.document.dto.SummaryResponse;
import com.document.repository.SummaryJobRepository;
import com.document.repository.SummaryResultRepository;
import com.document.application.cache.JobStatusCache;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class DocumentQueryService {

    private final SummaryJobRepository summaryJobRepository;
    private final SummaryResultRepository summaryResultRepository;
    private final JobStatusCache jobStatusCache;

    /**
     * 1. Redis Hash에서 조회
     * 2. Redis에 없으면 DB 조회
     */
    @Transactional(readOnly = true)
    public JobStatusResponse getJobStatus(final long jobId) {

        // 1. Redis에서 우선 조회
        Map<String, String> cachedStatus = jobStatusCache.getStatus(jobId);

        if (cachedStatus != null) {
            return JobStatusResponse.of(jobId , JobStatus.valueOf(cachedStatus.get("status")));
        }

        // 2. Redis에 없으면 DB에서 조회 (fallback)
        SummaryJob job = summaryJobRepository.findById(jobId)
                .orElseThrow(() -> DocumentException.from(DocumentErrorCode.DOCUMENT_NOT_FOUND));

        return JobStatusResponse.of(job.getId(), job.getStatus());
    }

    @Transactional(readOnly = true)
    public SummaryResponse getSummary(final long jobId) {
        SummaryJob job = summaryJobRepository.findById(jobId)
                .orElseThrow(() -> DocumentException.from(DocumentErrorCode.DOCUMENT_NOT_FOUND));

        SummaryResult result = summaryResultRepository.findByJobId(jobId)
                .orElse(null);
        String summary = result != null ? result.getSummary() : "문서 분석 결과 정보가 존재하지 않습니다.";

        return SummaryResponse.of(job.getId(), job.getStatus().getMessage(), summary);
    }
}
