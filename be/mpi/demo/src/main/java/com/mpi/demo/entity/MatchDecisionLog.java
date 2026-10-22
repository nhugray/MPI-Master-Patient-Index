package com.mpi.demo.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.mpi.demo.constant.MatchActionEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "match_decision_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchDecisionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_candidate_id", nullable = false)
    private MatchCandidate matchCandidate;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private MatchActionEnum action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private User performedBy;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "before_state", columnDefinition = "JSON")
    private String beforeState;

    @Column(name = "after_state", columnDefinition = "JSON")
    private String afterState;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
