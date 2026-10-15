package com.mpi.demo.service.impl;

import java.io.IOException;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mpi.demo.constant.ImportJobStatusEnum;
import com.mpi.demo.constant.ImportRowStatusEnum;
import com.mpi.demo.dto.request.ImportJobSearchRequest;
import com.mpi.demo.dto.request.StartImportRequest;
import com.mpi.demo.dto.request.UploadFileRequest;
import com.mpi.demo.dto.response.FileValidationResponse;
import com.mpi.demo.dto.response.ImportJobDetailResponse;
import com.mpi.demo.dto.response.ImportJobResponse;
import com.mpi.demo.dto.response.PreviewRowDto;
import com.mpi.demo.entity.ImportJob;
import com.mpi.demo.entity.ImportJobDetail;
import com.mpi.demo.entity.SourceSystem;
import com.mpi.demo.exception.ResourceNotFoundException;
import com.mpi.demo.repository.ImportJobDetailRepository;
import com.mpi.demo.repository.ImportJobRepository;
import com.mpi.demo.repository.SourceSystemRepository;
import com.mpi.demo.service.AsyncImportProcessor;
import com.mpi.demo.service.FileParserService;
import com.mpi.demo.service.FileStorageService;
import com.mpi.demo.service.FileValidationService;
import com.mpi.demo.service.ImportService;
import com.mpi.demo.service.ParsedFileData;
import com.mpi.demo.service.StoredFile;

@Service
public class ImportServiceImpl implements ImportService {
    private final ImportJobRepository importJobRepository;
    private final ImportJobDetailRepository detailRepository;
    private final SourceSystemRepository sourceSystemRepository;
    private final FileStorageService fileStorageService;
    private final FileParserService fileParserService;
    private final FileValidationService validationService;
    private final AsyncImportProcessor asyncImportProcessor;
    private final ObjectMapper objectMapper;

    public ImportServiceImpl(
            ImportJobRepository importJobRepository,
            ImportJobDetailRepository detailRepository,
            SourceSystemRepository sourceSystemRepository,
            FileStorageService fileStorageService,
            FileParserService fileParserService,
            FileValidationService validationService,
            AsyncImportProcessor asyncImportProcessor,
            ObjectMapper objectMapper) {
        this.importJobRepository = importJobRepository;
        this.detailRepository = detailRepository;
        this.sourceSystemRepository = sourceSystemRepository;
        this.fileStorageService = fileStorageService;
        this.fileParserService = fileParserService;
        this.validationService = validationService;
        this.asyncImportProcessor = asyncImportProcessor;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public FileValidationResponse uploadAndValidateFile(UploadFileRequest request) {
        SourceSystem sourceSystem = sourceSystemRepository.findById(request.getSourceSystemId())
                .orElseThrow(() -> new ResourceNotFoundException("Source system", "id", request.getSourceSystemId()));

        if (!Boolean.TRUE.equals(sourceSystem.getIsActive())) {
            throw new RuntimeException("Hệ thống nguồn đang bị vô hiệu hóa");
        }

        try {
            StoredFile storedFile = fileStorageService.store(request.getFile());

            ParsedFileData parsed = fileParserService.parse(storedFile.path(), storedFile.fileType());

            FileValidationResponse validation = validationService.validate(parsed, sourceSystem.getId());

            return new FileValidationResponse(
                    validation.isValid(),
                    storedFile.token(),
                    storedFile.originalFileName(),
                    storedFile.size(),
                    validation.totalRows(),
                    validation.validRows(),
                    validation.invalidRows(),
                    validation.detectedColumns(),
                    validation.suggestedMappings(),
                    validation.errors(),
                    validation.preview());
        } catch (IOException exception) {
            throw new RuntimeException("Không thể lưu hoặc đọc file import: " + exception.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PreviewRowDto> previewImportData(Long jobId, Pageable pageable) {
        ImportJob job = findJob(jobId);

        String fileToken = job.getFileToken() != null ? job.getFileToken() : job.getFileName();
        StoredFile storedFile = fileStorageService.getMetadata(fileToken);

        try {
            ParsedFileData parsed = fileParserService.parse(storedFile.path(), job.getFileType());
            FileValidationResponse validation = validationService.validate(parsed, job.getSourceSystem().getId());

            int start = Math.toIntExact(pageable.getOffset());
            int end = Math.min(start + pageable.getPageSize(), validation.preview().size());

            List<PreviewRowDto> content = start >= validation.preview().size()
                    ? List.of()
                    : validation.preview().subList(start, end);

            return new PageImpl<>(content, pageable, validation.preview().size());
        } catch (IOException exception) {
            throw new RuntimeException("Không thể đọc file preview: " + exception.getMessage());
        }
    }

    @Override
    @Transactional
    public ImportJobResponse startImport(StartImportRequest request) {
        return createImportJobOnly(request);
    }

    @Transactional
    public ImportJobResponse createImportJobOnly(StartImportRequest request) {

        SourceSystem sourceSystem = sourceSystemRepository.findById(request.getSourceSystemId())
                .orElseThrow(() -> new ResourceNotFoundException("Source system", "id", request.getSourceSystemId()));

        if (request.getColumnMappings() == null) {
            throw new IllegalArgumentException("Column mappings không được để trống");
        }

        if (request.getColumnMappings().isEmpty()) {
            throw new IllegalArgumentException("Column mappings không được để trống");
        }

        if (!request.getColumnMappings().containsKey("localPatientCode")
                && !request.getColumnMappings().containsKey("fullName")) {
            throw new IllegalArgumentException("Column mappings phải có ít nhất localPatientCode hoặc fullName");
        }

        StoredFile storedFile = fileStorageService.getMetadata(request.getFileToken());

        ImportJob job = ImportJob.builder()
                .fileToken(request.getFileToken())
                .fileName(storedFile.originalFileName())
                .fileSize(storedFile.size())
                .fileType(storedFile.fileType())
                .sourceSystem(sourceSystem)
                .status(ImportJobStatusEnum.PENDING)
                .totalRows(0)
                .processedRows(0)
                .successfulRows(0)
                .failedRows(0)
                .duplicateRows(0)
                .warningRows(0)
                .configuration(toJson(request.getColumnMappings()))
                .createdBy(1L)
                .build();

        ImportJob saved = importJobRepository.save(job);

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ImportJobResponse getImportJobStatus(Long jobId) {
        return toResponse(findJob(jobId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ImportJobDetailResponse> getImportJobDetails(Long jobId, ImportRowStatusEnum status,
            Pageable pageable) {
        findJob(jobId);

        Page<ImportJobDetail> page;
        if (status == null) {
            page = detailRepository.findByImportJobIdOrderByRowNumber(jobId, pageable);
        } else {
            page = detailRepository.findByImportJobIdAndStatus(jobId, status, pageable);
        }

        return page.map(ImportJobDetailResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ImportJobResponse> searchImportJobs(ImportJobSearchRequest request, Pageable pageable) {
        Page<ImportJob> page;

        if (request.status() != null) {
            page = importJobRepository.findByStatusOrderByCreatedAtDesc(request.status(), pageable);
        } else if (request.sourceSystemId() != null) {
            page = importJobRepository.findBySourceSystemIdOrderByCreatedAtDesc(request.sourceSystemId(), pageable);
        } else {
            page = importJobRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return page.map(this::toResponse);
    }

    @Override
    @Transactional
    public void cancelImportJob(Long jobId) {
        ImportJob job = findJob(jobId);

        if (job.getStatus() != ImportJobStatusEnum.PENDING
                && job.getStatus() != ImportJobStatusEnum.VALIDATING
                && job.getStatus() != ImportJobStatusEnum.PROCESSING) {
            throw new RuntimeException("Job không còn ở trạng thái có thể hủy");
        }

        job.setStatus(ImportJobStatusEnum.CANCELLED);
        importJobRepository.save(job);
    }

    @Override
    @Transactional
    public void retryFailedRows(Long jobId) {
        ImportJob job = findJob(jobId);

        if (job.getStatus() != ImportJobStatusEnum.COMPLETED
                && job.getStatus() != ImportJobStatusEnum.FAILED) {
            throw new RuntimeException("Chỉ được retry job đã kết thúc");
        }

        job.setStatus(ImportJobStatusEnum.PENDING);
        job.setErrorMessage(null);
        job.setProcessedRows(0);
        job.setSuccessfulRows(0);
        job.setFailedRows(0);
        job.setDuplicateRows(0);
        job.setWarningRows(0);

        importJobRepository.save(job);

        asyncImportProcessor.process(job.getId());
    }

    private ImportJob findJob(Long jobId) {
        return importJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Import job", "id", jobId));
    }

    private ImportJobResponse toResponse(ImportJob job) {
        int total = job.getTotalRows() == null ? 0 : job.getTotalRows();
        int processed = job.getProcessedRows() == null ? 0 : job.getProcessedRows();
        int progress = total == 0 ? 0 : Math.min(100, processed * 100 / total);

        return new ImportJobResponse(
                job.getId(),
                job.getFileName(),
                job.getFileSize(),
                job.getFileType(),
                job.getSourceSystem().getId(),
                job.getSourceSystem().getName(),
                job.getStatus(),
                job.getTotalRows(),
                job.getProcessedRows(),
                job.getSuccessfulRows(),
                job.getFailedRows(),
                job.getDuplicateRows(),
                job.getWarningRows(),
                progress,
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getErrorMessage(),
                job.getCreatedBy(),
                null,
                job.getCreatedAt());
    }

    private String toJson(java.util.Map<String, String> mappings) {
        try {
            return objectMapper.writeValueAsString(mappings);
        } catch (JsonProcessingException exception) {
            throw new RuntimeException("Không thể lưu cấu hình mapping");
        }
    }
}
