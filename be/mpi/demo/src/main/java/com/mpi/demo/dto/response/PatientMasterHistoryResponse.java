package com.mpi.demo.dto.response;

import java.time.LocalDateTime;

public record PatientMasterHistoryResponse(
                String action,
                String actionType,
                LocalDateTime date,
                String performedBy,
                String detail,
                String colorCode,
                Double matchScore) {
}
