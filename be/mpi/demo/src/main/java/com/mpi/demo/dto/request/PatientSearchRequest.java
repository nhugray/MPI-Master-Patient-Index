package com.mpi.demo.dto.request;

import com.mpi.demo.constant.GenderEnum;
import com.mpi.demo.constant.MatchStatusEnum;

public record PatientSearchRequest(
                String fullName,

                GenderEnum gender,

                String nationalId,

                String healthInsuranceNo,

                String phoneNumber,

                String address,

                Long sourceSystemId,

                String localPatientCode,

                MatchStatusEnum matchStatus

) {

}
