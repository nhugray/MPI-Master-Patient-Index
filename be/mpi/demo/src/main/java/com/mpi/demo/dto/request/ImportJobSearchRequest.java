package com.mpi.demo.dto.request;

import java.time.LocalDate;

import com.mpi.demo.constant.ImportJobStatusEnum;

public record ImportJobSearchRequest(
        ImportJobStatusEnum status,
        Long sourceSystemId,
        LocalDate fromDate,
        LocalDate toDate,
        Long createdBy) {
}
