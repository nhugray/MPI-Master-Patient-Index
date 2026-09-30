package com.mpi.demo.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.mpi.demo.constant.MatchDecisionEnum;
import com.mpi.demo.entity.MatchCandidate;

public interface MatchCandidateRepository extends JpaRepository<MatchCandidate, Long> {

    List<MatchCandidate> findByPatientId(Long patientId);

    Page<MatchCandidate> findByDecisionOrderByCreatedAtDesc(MatchDecisionEnum decision, Pageable pageable);

    @Query("SELECT mc FROM MatchCandidate mc WHERE " +
            "mc.matchScore >= ?1 AND " +
            "mc.decision = 'PENDING' " +
            "ORDER BY mc.matchScore DESC")
    Page<MatchCandidate> findHighScorePendingCandidates(BigDecimal minScore, Pageable pageable);

    @Query("SELECT COUNT(mc) FROM MatchCandidate mc WHERE mc.decision = 'PENDING'")
    long countPendingReviews();

    long countByDecision(MatchDecisionEnum decision);
}
