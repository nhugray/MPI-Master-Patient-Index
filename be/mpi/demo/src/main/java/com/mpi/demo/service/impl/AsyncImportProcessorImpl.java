package com.mpi.demo.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mpi.demo.constant.GenderEnum;
import com.mpi.demo.constant.ImportJobStatusEnum;
import com.mpi.demo.constant.ImportRowStatusEnum;
import com.mpi.demo.constant.MatchDecisionEnum;
import com.mpi.demo.constant.MatchStatusEnum;
import com.mpi.demo.entity.ImportJob;
import com.mpi.demo.entity.ImportJobDetail;
import com.mpi.demo.entity.MatchCandidate;
import com.mpi.demo.entity.Patient;
import com.mpi.demo.entity.SourceSystem;
import com.mpi.demo.exception.ResourceNotFoundException;
import com.mpi.demo.repository.ImportJobDetailRepository;
import com.mpi.demo.repository.ImportJobRepository;
import com.mpi.demo.repository.MatchCandidateRepository;
import com.mpi.demo.repository.PatientRepository;
import com.mpi.demo.repository.SourceSystemRepository;
import com.mpi.demo.service.AsyncImportProcessor;
import com.mpi.demo.service.FileParserService;
import com.mpi.demo.service.FileStorageService;
import com.mpi.demo.service.ParsedFileData;
import com.mpi.demo.service.PatientMasterService;
import com.mpi.demo.service.PatientMatchingService;
import com.mpi.demo.service.StoredFile;

@Service
public class AsyncImportProcessorImpl implements AsyncImportProcessor {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AsyncImportProcessorImpl.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ImportJobRepository importJobRepository;
    private final ImportJobDetailRepository detailRepository;
    private final PatientRepository patientRepository;
    private final SourceSystemRepository sourceSystemRepository;
    private final FileStorageService fileStorageService;
    private final FileParserService fileParserService;
    private final PatientMatchingService matchingService;
    private final PatientMasterService patientMasterService;
    private final MatchCandidateRepository matchCandidateRepository;
    private final ObjectMapper objectMapper;

    public AsyncImportProcessorImpl(
            ImportJobRepository importJobRepository,
            ImportJobDetailRepository detailRepository,
            PatientRepository patientRepository,
            SourceSystemRepository sourceSystemRepository,
            FileStorageService fileStorageService,
            FileParserService fileParserService,
            PatientMatchingService matchingService,
            PatientMasterService patientMasterService,
            MatchCandidateRepository matchCandidateRepository,
            ObjectMapper objectMapper) {
        this.importJobRepository = importJobRepository;
        this.detailRepository = detailRepository;
        this.patientRepository = patientRepository;
        this.sourceSystemRepository = sourceSystemRepository;
        this.fileStorageService = fileStorageService;
        this.fileParserService = fileParserService;
        this.matchingService = matchingService;
        this.patientMasterService = patientMasterService;
        this.matchCandidateRepository = matchCandidateRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Async("importTaskExecutor")
    public void process(Long importJobId) {
        try {
            processInternal(importJobId);
        } catch (Exception exception) {
            log.error("Import job {} failed", importJobId, exception);
            importJobRepository.findById(importJobId).ifPresent(job -> {
                job.setStatus(ImportJobStatusEnum.FAILED);
                job.setErrorMessage(exception.getMessage());
                job.setCompletedAt(LocalDateTime.now());
                importJobRepository.save(job);
            });
        }
    }

    @Transactional
    protected void processInternal(Long importJobId) throws Exception {
        ImportJob job = importJobRepository.findById(importJobId)
                .orElseThrow(() -> new ResourceNotFoundException("Import job", "id", importJobId));

        String fileToken = job.getFileToken() != null ? job.getFileToken() : job.getFileName();
        StoredFile storedFile = fileStorageService.getMetadata(fileToken);
        ParsedFileData data = fileParserService.parse(storedFile.path(), job.getFileType());

        SourceSystem sourceSystem = sourceSystemRepository.findById(job.getSourceSystem().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Source system", "id", job.getSourceSystem().getId()));

        job.setStatus(ImportJobStatusEnum.PROCESSING);
        job.setStartedAt(LocalDateTime.now());
        job.setTotalRows(data.rows().size());
        importJobRepository.save(job);

        Map<String, String> mappings = parseMappings(job.getConfiguration());

        for (int index = 0; index < data.rows().size(); index++) {
            if (isCancelled(job.getId())) {
                job.setStatus(ImportJobStatusEnum.CANCELLED);
                job.setCompletedAt(LocalDateTime.now());
                importJobRepository.save(job);
                return;
            }
            processRow(job, sourceSystem, data.rows().get(index), index + 2, mappings);
        }

        job.setStatus(ImportJobStatusEnum.COMPLETED);
        job.setCompletedAt(LocalDateTime.now());
        importJobRepository.save(job);
    }

    @Transactional
    protected void processRow(
            ImportJob job,
            SourceSystem sourceSystem,
            Map<String, String> row,
            int rowNumber,
            Map<String, String> mappings) {

        ImportJobDetail detail = ImportJobDetail.builder()
                .importJob(job)
                .rowNumber(rowNumber)
                .rowData(toJson(row))
                .status(ImportRowStatusEnum.PENDING)
                .build();

        try {
            String localCode = value(row, mappings, "localPatientCode");

            if (patientRepository.existsBySourceSystemIdAndLocalPatientCode(sourceSystem.getId(), localCode)) {
                detail.setStatus(ImportRowStatusEnum.DUPLICATE);
                detail.setErrorMessage("Mã bệnh nhân nội bộ đã tồn tại trong hệ thống");
                job.setDuplicateRows(job.getDuplicateRows() + 1);
            } else {
                Patient patient = toPatient(row, mappings, sourceSystem);
                patient = patientRepository.save(patient);
                detail.setPatient(patient);

                List<MatchCandidate> candidates = matchingService.findMatchCandidates(patient);

                if (candidates.isEmpty()) {
                    patientMasterService.createMaster(patient);
                    detail.setStatus(ImportRowStatusEnum.SUCCESS);
                    job.setSuccessfulRows(job.getSuccessfulRows() + 1);
                } else {
                    MatchCandidate topCandidate = candidates.get(0);

                    for (MatchCandidate candidate : candidates) {
                        candidate.setPatient(patient);
                        matchCandidateRepository.save(candidate);
                    }

                    detail.setMatchScore(topCandidate.getMatchScore());
                    detail.setMatchedMasterId(topCandidate.getCandidateMaster().getId());

                    if (topCandidate.getDecision() == MatchDecisionEnum.AUTO_APPROVED) {
                        patientMasterService.linkToMaster(patient.getId(), topCandidate.getCandidateMaster().getId());
                        detail.setStatus(ImportRowStatusEnum.SUCCESS);
                        job.setSuccessfulRows(job.getSuccessfulRows() + 1);
                    } else if (topCandidate.getDecision() == MatchDecisionEnum.PENDING) {
                        detail.setStatus(ImportRowStatusEnum.REQUIRES_REVIEW);
                        detail.setWarningMessage("Phát hiện trùng khớp với độ tin cậy "
                                + topCandidate.getMatchScore() + "%, cần review thủ công");
                        job.setWarningRows(job.getWarningRows() + 1);
                    } else {
                        patientMasterService.createMaster(patient);
                        detail.setStatus(ImportRowStatusEnum.SUCCESS);
                        job.setSuccessfulRows(job.getSuccessfulRows() + 1);
                    }
                }
            }
        } catch (Exception exception) {
            log.error("Import row {} failed", rowNumber, exception);
            detail.setStatus(ImportRowStatusEnum.FAILED);
            detail.setErrorMessage(exception.getMessage());
            job.setFailedRows(job.getFailedRows() + 1);
        }

        detailRepository.save(detail);
        job.setProcessedRows(job.getProcessedRows() + 1);
        importJobRepository.save(job);
    }

    private Patient toPatient(Map<String, String> row, Map<String, String> mappings, SourceSystem sourceSystem) {
        Patient patient = new Patient();
        patient.setSourceSystem(sourceSystem);
        patient.setLocalPatientCode(value(row, mappings, "localPatientCode"));
        patient.setFullName(value(row, mappings, "fullName"));
        patient.setNationalId(value(row, mappings, "nationalId"));
        patient.setHealthInsuranceNo(value(row, mappings, "healthInsuranceNo"));
        patient.setPhoneNumber(value(row, mappings, "phoneNumber"));
        patient.setAddress(value(row, mappings, "address"));
        patient.setGender(parseGender(value(row, mappings, "gender")));
        patient.setDateOfBirth(parseDate(value(row, mappings, "dateOfBirth")));
        patient.setMatchStatus(MatchStatusEnum.PENDING);
        return patient;
    }

    private boolean isCancelled(Long jobId) {
        return importJobRepository.findById(jobId)
                .map(job -> job.getStatus() == ImportJobStatusEnum.CANCELLED)
                .orElse(true);
    }

    private String value(Map<String, String> row, Map<String, String> mappings, String field) {
        String header = mappings.get(field);
        return header == null || row.get(header) == null ? "" : row.get(header).trim();
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private GenderEnum parseGender(String value) {
        if (value == null || value.isBlank()) {
            return GenderEnum.OTHER;
        }
        return switch (value.trim().toUpperCase()) {
            case "MALE", "NAM", "M" -> GenderEnum.MALE;
            case "FEMALE", "NU", "F", "NỮ" -> GenderEnum.FEMALE;
            case "OTHER", "KHAC", "KHÁC" -> GenderEnum.OTHER;
            default -> GenderEnum.OTHER;
        };
    }

    private Map<String, String> parseMappings(String configuration) {
        if (configuration == null || configuration.isBlank()) {
            Map<String, String> defaultMappings = new HashMap<>();
            defaultMappings.put("localPatientCode", "localPatientCode");
            defaultMappings.put("fullName", "fullName");
            defaultMappings.put("dateOfBirth", "dateOfBirth");
            defaultMappings.put("nationalId", "nationalId");
            defaultMappings.put("healthInsuranceNo", "healthInsuranceNo");
            defaultMappings.put("phoneNumber", "phoneNumber");
            defaultMappings.put("gender", "gender");
            defaultMappings.put("address", "address");
            return defaultMappings;
        }

        try {
            return objectMapper.readValue(configuration,
                    objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class));
        } catch (JsonProcessingException e) {
            Map<String, String> defaultMappings = new HashMap<>();
            defaultMappings.put("localPatientCode", "localPatientCode");
            defaultMappings.put("fullName", "fullName");
            defaultMappings.put("dateOfBirth", "dateOfBirth");
            defaultMappings.put("nationalId", "nationalId");
            defaultMappings.put("healthInsuranceNo", "healthInsuranceNo");
            defaultMappings.put("phoneNumber", "phoneNumber");
            defaultMappings.put("gender", "gender");
            defaultMappings.put("address", "address");
            return defaultMappings;
        }
    }

    private String toJson(Map<String, String> row) {
        try {
            return objectMapper.writeValueAsString(row);
        } catch (JsonProcessingException e) {
            return row.toString();
        }
    }
}
