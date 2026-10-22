package com.mpi.demo.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.mpi.demo.constant.GenderEnum;
import com.mpi.demo.constant.MatchStatusEnum;
import com.mpi.demo.entity.Patient;
import com.mpi.demo.entity.PatientMaster;

public record PatientResponse(
        Long id,
        Long sourceSystemId,
        String sourceSystemName,
        String localPatientCode,
        String fullName,
        LocalDate dateOfBirth,
        GenderEnum gender,
        String nationalId,
        String phoneNumber,
        String address,
        String healthInsuranceNo,
        Long masterPatientId,
        String enterpriseId,
        PatientMaster masterPatient,
        MatchStatusEnum matchStatus,
        BigDecimal matchScore) {

    public static PatientResponse fromEntity(Patient patient) {
        return fromEntity(patient, null);
    }

    public static PatientResponse fromEntity(Patient patient, BigDecimal matchScore) {
        return new PatientResponse(
                patient.getId(),
                patient.getSourceSystem() != null ? patient.getSourceSystem().getId() : null,
                patient.getSourceSystem() != null ? patient.getSourceSystem().getName() : null,
                patient.getLocalPatientCode(),
                patient.getFullName(),
                patient.getDateOfBirth(),
                patient.getGender(),
                patient.getNationalId(),
                patient.getPhoneNumber(),
                patient.getAddress(),
                patient.getHealthInsuranceNo(),
                patient.getMasterPatient() != null ? patient.getMasterPatient().getId() : null,
                patient.getMasterPatient() != null ? patient.getMasterPatient().getEnterpriseId() : null,
                patient.getMasterPatient(),
                patient.getMatchStatus(),
                matchScore);
    }
}
