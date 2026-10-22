package com.mpi.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mpi.demo.entity.MatchDecisionLog;

public interface MatchDecisionLogRepository extends JpaRepository<MatchDecisionLog, Long> {

    @Query("SELECT mdl FROM MatchDecisionLog mdl " +
            "JOIN FETCH mdl.matchCandidate mc " +
            "JOIN FETCH mc.patient p " +
            "LEFT JOIN FETCH p.sourceSystem ss " +
            "LEFT JOIN FETCH mdl.performedBy u " +
            "WHERE mc.candidateMaster.id = :masterId " +
            "ORDER BY mdl.createdAt DESC")
    List<MatchDecisionLog> findByMasterIdOrderByCreatedAtDesc(@Param("masterId") Long masterId);

    List<MatchDecisionLog> findByMatchCandidateId(Long matchCandidateId);
}
