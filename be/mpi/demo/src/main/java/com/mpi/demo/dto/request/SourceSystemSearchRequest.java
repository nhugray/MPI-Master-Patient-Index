package com.mpi.demo.dto.request;

public record SourceSystemSearchRequest(
        String name,

        String code,

        Long facilityId,

        Boolean isActive

) {

}
