package com.mpi.demo.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.mpi.demo.dto.request.ImportJobSearchRequest;
import com.mpi.demo.dto.request.StartImportRequest;
import com.mpi.demo.dto.request.UploadFileRequest;
import com.mpi.demo.dto.response.FileValidationResponse;
import com.mpi.demo.dto.response.ImportJobDetailResponse;
import com.mpi.demo.dto.response.ImportJobResponse;
import com.mpi.demo.dto.response.PreviewRowDto;
import com.mpi.demo.service.ImportService;

@Service
public class ImportServiceImpl implements ImportService {
    @Override
    public FileValidationResponse uploadAndValidateFile(UploadFileRequest request) {
        return null;
    }

    @Override
    public void cancelImportJob(Long jobId) {
    }

    @Override
    public void retryFailedRows(Long jobId) {
    }

    @Override
    public Page<PreviewRowDto> previewImportData(Long jobId, Pageable pageable) {
        return null;
    }

    @Override
    public ImportJobResponse startImport(StartImportRequest request) {
        return null;
    }

    @Override
    public ImportJobResponse getImportJobStatus(Long jobId) {
        return null;
    }

    @Override
    public Page<ImportJobResponse> searchImportJobs(ImportJobSearchRequest request, Pageable pageable) {
        return null;
    }

    @Override
    public Page<ImportJobDetailResponse> getImportJobDetails(Long jobId,
            com.mpi.demo.constant.ImportRowStatusEnum status,
            Pageable pageable) {
        return null;
    }

}
