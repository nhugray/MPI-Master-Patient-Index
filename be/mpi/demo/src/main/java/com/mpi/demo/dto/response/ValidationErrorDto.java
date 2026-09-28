package com.mpi.demo.dto.response;

public record ValidationErrorDto(
        Integer rowNumber,
        String column,
        String message,
        String severity) {
}
