package com.mpi.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mpi.demo.constant.ImportRowStatusEnum;
import com.mpi.demo.dto.request.ImportJobSearchRequest;
import com.mpi.demo.dto.request.StartImportRequest;
import com.mpi.demo.dto.request.UploadFileRequest;
import com.mpi.demo.dto.response.FileValidationResponse;
import com.mpi.demo.dto.response.ImportJobDetailResponse;
import com.mpi.demo.dto.response.ImportJobResponse;
import com.mpi.demo.dto.response.PreviewRowDto;

public interface ImportService {
    FileValidationResponse uploadAndValidateFile(UploadFileRequest request);

    Page<PreviewRowDto> previewImportData(Long jobId, Pageable pageable);

    ImportJobResponse startImport(StartImportRequest request);

    ImportJobResponse getImportJobStatus(Long jobId);

    Page<ImportJobDetailResponse> getImportJobDetails(Long jobId, ImportRowStatusEnum status, Pageable pageable);

    Page<ImportJobResponse> searchImportJobs(ImportJobSearchRequest request, Pageable pageable);

    void cancelImportJob(Long jobId);

    void retryFailedRows(Long jobId);
}
