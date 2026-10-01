package com.mpi.demo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "algorithm_weight_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlgorithmWeightConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "version", unique = true, nullable = false, length = 20)
    private String version; // Format: v2.4, v2.5...

    @Column(name = "name_weight", nullable = false, precision = 5, scale = 2)
    private BigDecimal nameWeight;

    @Column(name = "dob_weight", nullable = false, precision = 5, scale = 2)
    private BigDecimal dobWeight;

    @Column(name = "national_id_weight", nullable = false, precision = 5, scale = 2)
    private BigDecimal nationalIdWeight;

    @Column(name = "phone_weight", nullable = false, precision = 5, scale = 2)
    private BigDecimal phoneWeight;

    @Column(name = "address_weight", nullable = false, precision = 5, scale = 2)
    private BigDecimal addressWeight;

    @Column(name = "auto_approval_threshold", nullable = false, precision = 5, scale = 2)
    private BigDecimal autoApprovalThreshold;

    @Column(name = "manual_review_threshold", nullable = false, precision = 5, scale = 2)
    private BigDecimal manualReviewThreshold;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = false;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deployed_at")
    private LocalDateTime deployedAt;
}
