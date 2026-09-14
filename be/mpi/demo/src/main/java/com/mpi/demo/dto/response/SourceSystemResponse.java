package com.mpi.demo.dto.response;

import com.mpi.demo.entity.SourceSystem;

public record SourceSystemResponse(
        Long id,
        Long facilityId,
        String facilityName,
        String facilityCode,
        String code,
        String name,
        String description,
        Boolean isActive) {

    public static SourceSystemResponse fromEntity(SourceSystem sourceSystem) {
        return new SourceSystemResponse(
                sourceSystem.getId(),
                sourceSystem.getFacility().getId(),
                sourceSystem.getFacility().getName(),
                sourceSystem.getFacility().getCode(),
                sourceSystem.getCode(),
                sourceSystem.getName(),
                sourceSystem.getDescription(),
                sourceSystem.getIsActive());
    }
}
