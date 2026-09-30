package com.mpi.demo.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.mpi.demo.constant.MatchDecisionEnum;
import com.mpi.demo.entity.MatchCandidate;

public record MatchCandidateResponse(
        Long id,
        PatientResponse patient,
        PatientMasterResponse candidateMaster,
        BigDecimal matchScore,
        String scoreBreakdown,
        String weightVersion,
        MatchDecisionEnum decision,
        Long reviewedBy,
        String reviewedByName,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt) {

    public static MatchCandidateResponse fromEntity(MatchCandidate candidate) {
        return new MatchCandidateResponse(
                candidate.getId(),
                PatientResponse.fromEntity(candidate.getPatient()),
                PatientMasterResponse.fromEntity(candidate.getCandidateMaster()),
                candidate.getMatchScore(),
                candidate.getScoreBreakdown(),
                candidate.getWeightVersion(),
                candidate.getDecision(),
                candidate.getReviewedBy() != null ? candidate.getReviewedBy().getId() : null,
                candidate.getReviewedBy() != null ? candidate.getReviewedBy().getName() : null,
                candidate.getReviewedAt(),
                candidate.getCreatedAt());
    }
}
