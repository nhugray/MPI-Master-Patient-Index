package com.mpi.demo.dto.response;

import java.time.LocalDateTime;

import com.mpi.demo.constant.FileTypeEnum;
import com.mpi.demo.constant.ImportJobStatusEnum;

public record ImportJobResponse(
                Long id,
                String fileName,
                Long fileSize,
                FileTypeEnum fileType,
                Long sourceSystemId,
                String sourceSystemName,
                ImportJobStatusEnum status,
                Integer totalRows,
                Integer processedRows,
                Integer successfulRows,
                Integer failedRows,
                Integer duplicateRows,
                Integer warningRows,
                Integer progress,
                LocalDateTime startedAt,
                LocalDateTime completedAt,
                String errorMessage,
                Long createdBy,
                String createdByName,
                LocalDateTime createdAt) {
}