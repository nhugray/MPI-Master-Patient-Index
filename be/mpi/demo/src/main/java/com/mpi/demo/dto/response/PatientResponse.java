package com.mpi.demo.dto.response;

import java.time.LocalDate;

import com.mpi.demo.constant.GenderEnum;
import com.mpi.demo.constant.PatientStatusEnum;
import com.mpi.demo.entity.Patient;

public record PatientResponse(
        Long id,
        String fullName,
        LocalDate dateOfBirth,
        GenderEnum gender,
        String nationalId,
        String phoneNumber,
        PatientStatusEnum status,
        String healthInsuranceNo) {

    public static PatientResponse fromEntity(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getFullName(),
                patient.getDateOfBirth(),
                patient.getGender(),
                patient.getNationalId(),
                patient.getPhoneNumber(),
                patient.getStatus(),
                patient.getHealthInsuranceNo());
    }
}
