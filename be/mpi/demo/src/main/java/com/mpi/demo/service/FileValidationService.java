package com.mpi.demo.service;

import com.mpi.demo.dto.response.FileValidationResponse;

public interface FileValidationService {
    FileValidationResponse validate(ParsedFileData data, Long sourceSystemId);
}
