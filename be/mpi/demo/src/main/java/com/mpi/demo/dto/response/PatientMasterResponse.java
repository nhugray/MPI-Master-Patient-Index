package com.mpi.demo.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.mpi.demo.constant.GenderEnum;
import com.mpi.demo.constant.PatientStatusEnum;
import com.mpi.demo.entity.PatientMaster;

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

    public static PatientMasterResponse fromEntity(PatientMaster master) {
        return new PatientMasterResponse(
                master.getId(),
                master.getEnterpriseId(),
                master.getFullName(),
                master.getDateOfBirth(),
                master.getGender(),
                master.getNationalId(),
                master.getHealthInsuranceNo(),
                master.getPhoneNumber(),
                master.getAddress(),
                master.getStatus(),
                master.getMergedIntoId(),
                null,
                master.getCreatedAt(),
                master.getUpdatedAt());
    }
}
