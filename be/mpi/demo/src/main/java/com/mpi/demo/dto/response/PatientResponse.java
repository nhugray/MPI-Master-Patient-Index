package com.mpi.demo.dto.response;

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
        PatientMaster masterPatient,
        MatchStatusEnum matchStatus) {

    public static PatientResponse fromEntity(Patient patient) {
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
                patient.getMasterPatient(),
                patient.getMatchStatus());
    }
}
