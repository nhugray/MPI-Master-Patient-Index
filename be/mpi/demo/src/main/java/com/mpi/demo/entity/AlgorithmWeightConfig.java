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
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "algorithm_weight_config")
@Getter
@Setter
public class AlgorithmWeightConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "version", unique = true, nullable = false, length = 20)
    private String version;

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

    public AlgorithmWeightConfig() {
        super();
    }

    public AlgorithmWeightConfig(Long id, String version, BigDecimal nameWeight, BigDecimal dobWeight,
            BigDecimal nationalIdWeight, BigDecimal phoneWeight, BigDecimal addressWeight,
            BigDecimal autoApprovalThreshold, BigDecimal manualReviewThreshold,
            Boolean isActive, String description, String createdBy,
            LocalDateTime createdAt, LocalDateTime deployedAt) {
        this.id = id;
        this.version = version;
        this.nameWeight = nameWeight;
        this.dobWeight = dobWeight;
        this.nationalIdWeight = nationalIdWeight;
        this.phoneWeight = phoneWeight;
        this.addressWeight = addressWeight;
        this.autoApprovalThreshold = autoApprovalThreshold;
        this.manualReviewThreshold = manualReviewThreshold;
        this.isActive = isActive != null ? isActive : false;
        this.description = description;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.deployedAt = deployedAt;
    }

    public static AlgorithmWeightConfigBuilder builder() {
        return new AlgorithmWeightConfigBuilder();
    }

    public static class AlgorithmWeightConfigBuilder {
        private Long id;
        private String version;
        private BigDecimal nameWeight;
        private BigDecimal dobWeight;
        private BigDecimal nationalIdWeight;
        private BigDecimal phoneWeight;
        private BigDecimal addressWeight;
        private BigDecimal autoApprovalThreshold;
        private BigDecimal manualReviewThreshold;
        private Boolean isActive = false;
        private String description;
        private String createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime deployedAt;

        AlgorithmWeightConfigBuilder() {
        }

        public AlgorithmWeightConfigBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public AlgorithmWeightConfigBuilder version(String version) {
            this.version = version;
            return this;
        }

        public AlgorithmWeightConfigBuilder nameWeight(BigDecimal nameWeight) {
            this.nameWeight = nameWeight;
            return this;
        }

        public AlgorithmWeightConfigBuilder dobWeight(BigDecimal dobWeight) {
            this.dobWeight = dobWeight;
            return this;
        }

        public AlgorithmWeightConfigBuilder nationalIdWeight(BigDecimal nationalIdWeight) {
            this.nationalIdWeight = nationalIdWeight;
            return this;
        }

        public AlgorithmWeightConfigBuilder phoneWeight(BigDecimal phoneWeight) {
            this.phoneWeight = phoneWeight;
            return this;
        }

        public AlgorithmWeightConfigBuilder addressWeight(BigDecimal addressWeight) {
            this.addressWeight = addressWeight;
            return this;
        }

        public AlgorithmWeightConfigBuilder autoApprovalThreshold(BigDecimal autoApprovalThreshold) {
            this.autoApprovalThreshold = autoApprovalThreshold;
            return this;
        }

        public AlgorithmWeightConfigBuilder manualReviewThreshold(BigDecimal manualReviewThreshold) {
            this.manualReviewThreshold = manualReviewThreshold;
            return this;
        }

        public AlgorithmWeightConfigBuilder isActive(Boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public AlgorithmWeightConfigBuilder description(String description) {
            this.description = description;
            return this;
        }

        public AlgorithmWeightConfigBuilder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public AlgorithmWeightConfigBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public AlgorithmWeightConfigBuilder deployedAt(LocalDateTime deployedAt) {
            this.deployedAt = deployedAt;
            return this;
        }

        public AlgorithmWeightConfig build() {
            return new AlgorithmWeightConfig(id, version, nameWeight, dobWeight, nationalIdWeight, phoneWeight,
                    addressWeight, autoApprovalThreshold, manualReviewThreshold, isActive, description, createdBy,
                    createdAt, deployedAt);
        }
    }
}
