package com.mpi.demo.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.mpi.demo.entity.AlgorithmWeightConfig;

public record WeightConfigResponse(
        Long id,
        String version,
        BigDecimal nameWeight,
        BigDecimal dobWeight,
        BigDecimal nationalIdWeight,
        BigDecimal phoneWeight,
        BigDecimal addressWeight,
        BigDecimal autoApprovalThreshold,
        BigDecimal manualReviewThreshold,
        Boolean isActive,
        String description,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime deployedAt) {

    public static WeightConfigResponse fromEntity(AlgorithmWeightConfig config) {
        return new WeightConfigResponse(
                config.getId(),
                config.getVersion(),
                config.getNameWeight(),
                config.getDobWeight(),
                config.getNationalIdWeight(),
                config.getPhoneWeight(),
                config.getAddressWeight(),
                config.getAutoApprovalThreshold(),
                config.getManualReviewThreshold(),
                config.getIsActive(),
                config.getDescription(),
                config.getCreatedBy(),
                config.getCreatedAt(),
                config.getDeployedAt());
    }
}
