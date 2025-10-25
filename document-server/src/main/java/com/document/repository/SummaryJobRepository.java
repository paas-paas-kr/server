package com.document.repository;

import com.document.domain.SummaryJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SummaryJobRepository extends JpaRepository<SummaryJob, Long> {

	List<SummaryJob> findByUserId(Long userId);
}
