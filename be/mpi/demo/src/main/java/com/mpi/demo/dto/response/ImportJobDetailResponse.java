package com.mpi.demo.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.mpi.demo.constant.ImportRowStatusEnum;
import com.mpi.demo.entity.ImportJobDetail;

public record ImportJobDetailResponse(
        Long id,
        Integer rowNumber,
        String rowData,
        ImportRowStatusEnum status,
        BigDecimal matchScore,
        Long patientId,
        Long matchedMasterId,
        String errorMessage,
        String warningMessage,
        LocalDateTime processedAt) {

    public static ImportJobDetailResponse fromEntity(ImportJobDetail detail) {
        return new ImportJobDetailResponse(
                detail.getId(),
                detail.getRowNumber(),
                detail.getRowData(),
                detail.getStatus(),
                detail.getMatchScore(),
                detail.getPatient() == null ? null : detail.getPatient().getId(),
                detail.getMatchedMasterId(),
                detail.getErrorMessage(),
                detail.getWarningMessage(),
                detail.getProcessedAt());
    }
}
