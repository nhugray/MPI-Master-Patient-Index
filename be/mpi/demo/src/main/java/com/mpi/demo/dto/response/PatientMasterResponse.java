package com.mpi.demo.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.mpi.demo.constant.GenderEnum;
import com.mpi.demo.constant.PatientStatusEnum;

public record PatientMasterResponse(
        Long id,
        String enterpriseId,
        String fullName,
        LocalDate dateOfBirth,
        GenderEnum gender,
        String nationalId,
        String healthInsuranceNo,
        String phoneNumber,
        String address,
        PatientStatusEnum status,
        Long mergedIntoId,
        Long linkedPatientsCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
