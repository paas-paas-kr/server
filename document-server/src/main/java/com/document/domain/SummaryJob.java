package com.document.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.common.enumtype.Language;
import com.document.domain.enumtype.JobStatus;

@Entity
@Table(name = "summary_job")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SummaryJob {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@OneToMany(mappedBy = "job", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<Document> documents = new ArrayList<>();

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private JobStatus status;

	@Enumerated(EnumType.STRING)
	@Column
	private Language summaryLanguage; // 요약 언어

	@Column
	private String statusMessage; // 현재 진행 상태 메시지

	@Column
	private LocalDateTime startedAt;

	@Column
	private LocalDateTime completedAt;

	@Builder(access = AccessLevel.PRIVATE)
	private SummaryJob(Long userId, Language summaryLanguage, JobStatus status) {
		this.userId = userId;
		this.summaryLanguage = summaryLanguage;
		this.status = status;
		this.statusMessage = status.getMessage();
		this.startedAt = LocalDateTime.now();
	}

	public static SummaryJob of(Long userId, Language summaryLanguage) {
		return SummaryJob.builder()
			.userId(userId)
			.summaryLanguage(summaryLanguage)
			.status(JobStatus.PENDING)
			.build();
	}

	public void updateStatus(final JobStatus status) {
		this.status = status;
		this.statusMessage = status.getMessage();

		if (status == JobStatus.COMPLETED || status == JobStatus.FAILED) {
			this.completedAt = LocalDateTime.now();
		}
	}
}
