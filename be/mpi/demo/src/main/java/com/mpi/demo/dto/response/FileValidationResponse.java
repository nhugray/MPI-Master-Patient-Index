package com.mpi.demo.dto.response;

import java.util.List;
import java.util.Map;

public record FileValidationResponse(
        Boolean isValid,
        String fileToken,
        String fileName,
        Long fileSize,
        Integer totalRows,
        Integer validRows,
        Integer invalidRows,
        List<String> detectedColumns,
        Map<String, String> suggestedMappings,
        List<ValidationErrorDto> errors,
        List<PreviewRowDto> preview) {
}
