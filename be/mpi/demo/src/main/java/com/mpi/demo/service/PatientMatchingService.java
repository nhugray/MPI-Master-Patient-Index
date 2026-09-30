package com.mpi.demo.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mpi.demo.constant.MatchDecisionEnum;
import com.mpi.demo.entity.MatchCandidate;
import com.mpi.demo.entity.Patient;
import com.mpi.demo.entity.PatientMaster;

public interface PatientMatchingService {
    List<MatchCandidate> findMatchCandidates(Patient patient);

    BigDecimal calculateMatchScore(Patient patient, PatientMaster master);

    MatchDecisionEnum getAutoDecision(BigDecimal matchScore);

    void processMatchDecision(Long candidateId, MatchDecisionEnum decision, Long reviewerId, String reviewNote);

    Page<MatchCandidate> getPendingReviews(Pageable pageable);
    
    Page<MatchCandidate> searchCandidates(MatchDecisionEnum decision, Pageable pageable);
    
    MatchCandidate getCandidateById(Long candidateId);
    
    Map<String, Long> getDecisionCounts();
}
