package com.mpi.demo.dto.response;

import com.mpi.demo.constant.FacilityTypeEnum;
import com.mpi.demo.entity.Facility;

public record FacilityResponse(
        Long id,
        String code,
        String name,
        FacilityTypeEnum facilityType,
        String address,
        String phoneNumber,
        Boolean isActive) {

    public static FacilityResponse fromEntity(Facility facility) {
        return new FacilityResponse(
                facility.getId(),
                facility.getCode(),
                facility.getName(),
                facility.getFacilityType(),
                facility.getAddress(),
                facility.getPhoneNumber(),
                facility.getIsActive());
    }
}
