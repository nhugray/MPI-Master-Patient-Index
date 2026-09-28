package com.mpi.demo.service.impl;

import org.springframework.stereotype.Service;

import com.mpi.demo.dto.response.FileValidationResponse;
import com.mpi.demo.service.FileValidationService;
import com.mpi.demo.service.ParsedFileData;

@Service
public class FileValidationServiceImpl implements FileValidationService {

    @Override
    public FileValidationResponse validate(ParsedFileData data, Long sourceSystemId) {
        return null;
    }

}
