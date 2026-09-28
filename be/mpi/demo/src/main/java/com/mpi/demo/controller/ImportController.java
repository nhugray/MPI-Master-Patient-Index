package com.mpi.demo.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mpi.demo.constant.ImportRowStatusEnum;
import com.mpi.demo.dto.request.ImportJobSearchRequest;
import com.mpi.demo.dto.request.StartImportRequest;
import com.mpi.demo.dto.request.UploadFileRequest;
import com.mpi.demo.dto.response.FileValidationResponse;
import com.mpi.demo.dto.response.ImportJobDetailResponse;
import com.mpi.demo.dto.response.ImportJobResponse;
import com.mpi.demo.dto.response.PreviewRowDto;
import com.mpi.demo.helper.ApiResponse;
import com.mpi.demo.service.ImportService;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/api/v1/imports")
public class ImportController {
    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileValidationResponse>> upload(
            @Valid @ModelAttribute UploadFileRequest request) {

        return ResponseEntity
                .ok(ApiResponse.success("Tải tệp lên thành công", importService.uploadAndValidateFile(request)));
    }

    @GetMapping("/{jobId}/preview")
    public ResponseEntity<ApiResponse<Page<PreviewRowDto>>> review(@PathVariable Long jobId,
            @ParameterObject Pageable pageable) {
        return ResponseEntity
                .ok(ApiResponse.success(importService.previewImportData(jobId, pageable)));
    }

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<ImportJobResponse>> start(
            @Valid @RequestBody StartImportRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đã khởi động tải tệp", importService.startImport(request)));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<ApiResponse<ImportJobResponse>> status(
            @PathVariable Long jobId) {
        return ResponseEntity
                .ok(ApiResponse.success("Lấy trạng thái thành công", importService.getImportJobStatus(jobId)));
    }

    @GetMapping("/{jobId}/details")
    public ResponseEntity<ApiResponse<Page<ImportJobDetailResponse>>> details(
            @PathVariable Long jobId,
            @RequestParam(required = false) ImportRowStatusEnum status,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lay chi tiet import thanh cong",
                importService.getImportJobDetails(jobId, status, pageable)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ImportJobResponse>>> search(
            @ParameterObject ImportJobSearchRequest request,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                "Tim kiem import job thanh cong", importService.searchImportJobs(request, pageable)));
    }

    @PostMapping("/{jobId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long jobId) {
        importService.cancelImportJob(jobId);
        return ResponseEntity.ok(ApiResponse.success("Da huy import job", null));
    }

    @PostMapping("/{jobId}/retry-failed")
    public ResponseEntity<ApiResponse<Void>> retryFailed(@PathVariable Long jobId) {
        importService.retryFailedRows(jobId);
        return ResponseEntity.ok(ApiResponse.success("Da retry cac row loi", null));
    }
}
