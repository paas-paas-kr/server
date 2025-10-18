package com.document.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "document")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Document {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String fileName;

	@Column(nullable = false)
	private String filePath;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "job_id")
	private SummaryJob job;

	@Builder(access = AccessLevel.PRIVATE)
	private Document(String fileName, String filePath, SummaryJob job) {
		this.fileName = fileName;
		this.filePath = filePath;
		setJob(job);
	}

	public static Document of(final String fileName, final String filePath, final SummaryJob job) {
		return Document.builder()
			.fileName(fileName)
			.filePath(filePath)
			.job(job)
			.build();
	}

	public void setJob(SummaryJob job) {
		this.job = job;
		if (job != null && !job.getDocuments().contains(this)) {
			job.getDocuments().add(this);
		}
	}
}
