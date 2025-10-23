package com.document.repository;

import com.document.domain.SummaryResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SummaryResultRepository extends JpaRepository<SummaryResult, Long> {
    Optional<SummaryResult> findByJobId(Long jobId);
}
