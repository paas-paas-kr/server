package com.document.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "summary_result")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SummaryResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "job_id", nullable = false, unique = true)
    private SummaryJob job;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String summary;

    @Builder(access = AccessLevel.PRIVATE)
    private SummaryResult(SummaryJob job, String summary) {
        this.job = job;
        this.summary = summary;
    }

    public static SummaryResult of(final SummaryJob job, final String summary) {
        return SummaryResult.builder()
                .job(job)
                .summary(summary)
                .build();
    }
}
