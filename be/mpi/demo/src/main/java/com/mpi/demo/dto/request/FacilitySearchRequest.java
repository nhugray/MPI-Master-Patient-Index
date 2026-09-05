package com.mpi.demo.dto.request;

import com.mpi.demo.constant.FacilityTypeEnum;

public record FacilitySearchRequest(
        String name,

        String code,

        FacilityTypeEnum facilityType,

        Boolean isActive

) {

}
