package com.mpi.demo.dto.request;

import com.mpi.demo.constant.GenderEnum;
import com.mpi.demo.constant.PatientStatusEnum;

public record PatientMasterSearchRequest(
        String keyword,
        String enterpriseId,
        String nationalId,
        String healthInsuranceNo,
        String phoneNumber,
        GenderEnum gender,
        PatientStatusEnum status,
        Integer ageFrom,
        Integer ageTo) {
}
