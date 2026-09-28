package com.mpi.demo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import com.mpi.demo.constant.MatchDecisionEnum;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "match_candidate")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchCandidate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
   
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_master_id", nullable = false)
    private PatientMaster candidateMaster;
    
    @Column(name = "match_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal matchScore;
    
    @Column(name = "score_breakdown", columnDefinition = "JSON")
    private String scoreBreakdown; 
    
    @Column(name = "weight_version", nullable = false, length = 20)
    @Builder.Default
    private String weightVersion = "v1";
    
    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 20)
    @Builder.Default
    private MatchDecisionEnum decision = MatchDecisionEnum.PENDING;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;
    
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}