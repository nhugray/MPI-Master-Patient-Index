package com.mpi.demo.dto.response;

import java.util.List;
import java.util.Map;

public record PreviewRowDto(
        Integer rowNumber,
        Map<String, Object> data,
        String validationStatus,
        List<String> validationMessages,
        Double matchScore) {
}
