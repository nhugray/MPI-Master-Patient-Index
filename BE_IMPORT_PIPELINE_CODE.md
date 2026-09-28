# MPI Backend - Muc 5: Import Pipeline

> Tai lieu nay chi chua code de ap dung thu cong. Khong co file Java nao duoc tao hoac sua boi tai lieu nay.
> Cac doan code duoi day dung package hien tai: `com.mpi.demo`.

## 0. Tong quan file

### File tao moi

```text
src/main/java/com/mpi/demo/config/AsyncConfig.java
src/main/java/com/mpi/demo/controller/ImportController.java
src/main/java/com/mpi/demo/exception/FileParsingException.java
src/main/java/com/mpi/demo/exception/FileStorageException.java
src/main/java/com/mpi/demo/exception/ImportStateException.java
src/main/java/com/mpi/demo/service/AsyncImportProcessor.java
src/main/java/com/mpi/demo/service/FileParserService.java
src/main/java/com/mpi/demo/service/FileStorageService.java
src/main/java/com/mpi/demo/service/FileValidationService.java
src/main/java/com/mpi/demo/service/ParsedFileData.java
src/main/java/com/mpi/demo/service/StoredFile.java
src/main/java/com/mpi/demo/service/impl/AsyncImportProcessorImpl.java
src/main/java/com/mpi/demo/service/impl/FileParserServiceImpl.java
src/main/java/com/mpi/demo/service/impl/FileStorageServiceImpl.java
src/main/java/com/mpi/demo/service/impl/FileValidationServiceImpl.java
src/main/java/com/mpi/demo/dto/response/ImportJobDetailResponse.java
```

### File thay the noi dung

```text
src/main/java/com/mpi/demo/service/ImportService.java
src/main/java/com/mpi/demo/service/impl/ImportServiceImpl.java
src/main/java/com/mpi/demo/controller/ImportController.java
```

### File can bo sung/sua theo cac block cuoi tai lieu

```text
src/main/java/com/mpi/demo/entity/ImportJob.java
src/main/java/com/mpi/demo/entity/ImportJobDetail.java
src/main/java/com/mpi/demo/constant/ImportRowStatusEnum.java
src/main/java/com/mpi/demo/repository/ImportJobRepository.java
src/main/java/com/mpi/demo/repository/ImportJobDetailRepository.java
src/main/resources/application.yaml
pom.xml
```

---

## 1. File storage

### `service/StoredFile.java`

```java
package com.mpi.demo.service;

import java.nio.file.Path;

import com.mpi.demo.constant.FileTypeEnum;

public record StoredFile(
        String token,
        String originalFileName,
        long size,
        FileTypeEnum fileType,
        Path path) {
}
```

### `service/FileStorageService.java`

```java
package com.mpi.demo.service;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.web.multipart.MultipartFile;

import com.mpi.demo.constant.FileTypeEnum;

public interface FileStorageService {
    StoredFile store(MultipartFile file) throws IOException;

    InputStream open(String fileToken) throws IOException;

    StoredFile getMetadata(String fileToken);

    FileTypeEnum detectType(String fileName);

    void delete(String fileToken) throws IOException;
}
```

### `exception/FileStorageException.java`

```java
package com.mpi.demo.exception;

public class FileStorageException extends RuntimeException {
    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### `service/impl/FileStorageServiceImpl.java`

```java
package com.mpi.demo.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.mpi.demo.constant.FileTypeEnum;
import com.mpi.demo.exception.FileStorageException;
import com.mpi.demo.service.FileStorageService;
import com.mpi.demo.service.StoredFile;

@Service
public class FileStorageServiceImpl implements FileStorageService {
    private final Path uploadDirectory;
    private final long maxFileSize;
    private final Map<String, StoredFile> metadata = new ConcurrentHashMap<>();

    public FileStorageServiceImpl(
            @Value("${file.upload.dir:./uploads/imports}") String uploadDirectory,
            @Value("${file.upload.max-size:524288000}") long maxFileSize) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
        this.maxFileSize = maxFileSize;
        try {
            Files.createDirectories(this.uploadDirectory);
        } catch (IOException exception) {
            throw new FileStorageException("Khong the tao thu muc luu file import", exception);
        }
    }

    @Override
    public StoredFile store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("File import khong duoc rong");
        }
        if (file.getSize() > maxFileSize) {
            throw new FileStorageException("File vuot qua gioi han kich thuoc cho phep");
        }

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "import" : file.getOriginalFilename());
        FileTypeEnum type = detectType(originalName);
        String token = UUID.randomUUID().toString();
        Path target = uploadDirectory.resolve(token + extension(type)).normalize();

        if (!target.getParent().equals(uploadDirectory)) {
            throw new FileStorageException("Duong dan file khong hop le");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }

        StoredFile storedFile = new StoredFile(token, originalName, file.getSize(), type, target);
        metadata.put(token, storedFile);
        return storedFile;
    }

    @Override
    public InputStream open(String fileToken) throws IOException {
        return Files.newInputStream(getMetadata(fileToken).path());
    }

    @Override
    public StoredFile getMetadata(String fileToken) {
        StoredFile storedFile = metadata.get(fileToken);
        if (storedFile == null || !Files.isRegularFile(storedFile.path())) {
            throw new FileStorageException("Khong tim thay file import voi token: " + fileToken);
        }
        return storedFile;
    }

    @Override
    public FileTypeEnum detectType(String fileName) {
        String extension = StringUtils.getFilenameExtension(fileName);
        if (extension == null) {
            throw new FileStorageException("File phai co duoi .csv hoac .xlsx");
        }
        return switch (extension.toLowerCase(Locale.ROOT)) {
            case "csv" -> FileTypeEnum.CSV;
            case "xlsx" -> FileTypeEnum.XLSX;
            default -> throw new FileStorageException("Chi ho tro file CSV va XLSX");
        };
    }

    @Override
    public void delete(String fileToken) throws IOException {
        StoredFile storedFile = metadata.remove(fileToken);
        if (storedFile != null) {
            Files.deleteIfExists(storedFile.path());
        }
    }

    private String extension(FileTypeEnum type) {
        return type == FileTypeEnum.CSV ? ".csv" : ".xlsx";
    }
}
```

> Ghi chu: metadata hien duoc luu trong memory. Neu can restart-safe, tao them entity `ImportFile` hoac luu `fileToken/filePath` trong `ImportJob`.

---

## 2. Parser CSV/XLSX

### `service/ParsedFileData.java`

```java
package com.mpi.demo.service;

import java.util.List;
import java.util.Map;

import com.mpi.demo.constant.FileTypeEnum;

public record ParsedFileData(
        List<String> headers,
        List<Map<String, String>> rows,
        FileTypeEnum fileType) {
}
```

### `service/FileParserService.java`

```java
package com.mpi.demo.service;

import java.io.IOException;
import java.nio.file.Path;

import com.mpi.demo.constant.FileTypeEnum;

public interface FileParserService {
    ParsedFileData parse(Path path, FileTypeEnum fileType) throws IOException;
}
```

### `exception/FileParsingException.java`

```java
package com.mpi.demo.exception;

public class FileParsingException extends RuntimeException {
    public FileParsingException(String message) {
        super(message);
    }

    public FileParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### `service/impl/FileParserServiceImpl.java`

```java
package com.mpi.demo.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.mpi.demo.constant.FileTypeEnum;
import com.mpi.demo.exception.FileParsingException;
import com.mpi.demo.service.FileParserService;
import com.mpi.demo.service.ParsedFileData;

@Service
public class FileParserServiceImpl implements FileParserService {
    private static final int MAX_ROWS = 500_000;

    @Override
    public ParsedFileData parse(Path path, FileTypeEnum fileType) throws IOException {
        if (fileType == FileTypeEnum.CSV) {
            return parseCsv(path);
        }
        if (fileType == FileTypeEnum.XLSX) {
            return parseXlsx(path);
        }
        throw new FileParsingException("Loai file khong duoc ho tro: " + fileType);
    }

    private ParsedFileData parseCsv(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
                CSVReader csvReader = new CSVReader(reader)) {
            String[] rawHeaders = csvReader.readNext();
            if (rawHeaders == null || rawHeaders.length == 0) {
                throw new FileParsingException("File CSV khong co header");
            }

            List<String> headers = normalizeHeaders(List.of(rawHeaders));
            List<Map<String, String>> rows = new ArrayList<>();
            String[] values;
            int rowNumber = 1;
            while ((values = csvReader.readNext()) != null) {
                rowNumber++;
                if (rowNumber > MAX_ROWS + 1) {
                    throw new FileParsingException("File vuot qua gioi han " + MAX_ROWS + " dong");
                }
                rows.add(toRow(headers, values));
            }
            return new ParsedFileData(headers, rows, FileTypeEnum.CSV);
        } catch (CsvValidationException exception) {
            throw new FileParsingException("CSV khong dung dinh dang", exception);
        }
    }

    private ParsedFileData parseXlsx(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path);
                Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new FileParsingException("File Excel khong co worksheet");
            }
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null || headerRow.getLastCellNum() <= 0) {
                throw new FileParsingException("File Excel khong co header");
            }

            DataFormatter formatter = new DataFormatter();
            List<String> rawHeaders = new ArrayList<>();
            for (int index = 0; index < headerRow.getLastCellNum(); index++) {
                rawHeaders.add(formatter.formatCellValue(headerRow.getCell(index)));
            }
            List<String> headers = normalizeHeaders(rawHeaders);
            List<Map<String, String>> rows = new ArrayList<>();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                if (rows.size() >= MAX_ROWS) {
                    throw new FileParsingException("File vuot qua gioi han " + MAX_ROWS + " dong");
                }
                Row row = sheet.getRow(rowIndex);
                if (row != null) {
                    rows.add(toRow(headers, row, formatter));
                }
            }
            return new ParsedFileData(headers, rows, FileTypeEnum.XLSX);
        } catch (FileParsingException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new FileParsingException("Khong the doc file Excel", exception);
        }
    }

    private Map<String, String> toRow(List<String> headers, String[] values) {
        Map<String, String> row = new LinkedHashMap<>();
        for (int index = 0; index < headers.size(); index++) {
            row.put(headers.get(index), index < values.length ? values[index].trim() : "");
        }
        return row;
    }

    private Map<String, String> toRow(List<String> headers, Row row, DataFormatter formatter) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int index = 0; index < headers.size(); index++) {
            Cell cell = row.getCell(index);
            result.put(headers.get(index), cell == null ? "" : formatter.formatCellValue(cell).trim());
        }
        return result;
    }

    private List<String> normalizeHeaders(List<String> rawHeaders) {
        List<String> headers = new ArrayList<>();
        for (String rawHeader : rawHeaders) {
            String header = rawHeader == null ? "" : rawHeader.replace("\uFEFF", "").trim();
            if (header.isBlank()) {
                throw new FileParsingException("Header khong duoc de trong");
            }
            if (headers.contains(header)) {
                throw new FileParsingException("Header bi trung: " + header);
            }
            headers.add(header);
        }
        return headers;
    }
}
```

---

## 3. File validation

### `service/FileValidationService.java`

```java
package com.mpi.demo.service;

import com.mpi.demo.dto.response.FileValidationResponse;

public interface FileValidationService {
    FileValidationResponse validate(ParsedFileData data, Long sourceSystemId);
}
```

### `service/impl/FileValidationServiceImpl.java`

```java
package com.mpi.demo.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.mpi.demo.dto.response.FileValidationResponse;
import com.mpi.demo.dto.response.PreviewRowDto;
import com.mpi.demo.dto.response.ValidationErrorDto;
import com.mpi.demo.repository.PatientRepository;
import com.mpi.demo.service.FileValidationService;
import com.mpi.demo.service.ParsedFileData;

@Service
public class FileValidationServiceImpl implements FileValidationService {
    private static final Pattern PHONE = Pattern.compile("^0\\d{9,10}$");
    private static final Pattern NATIONAL_ID = Pattern.compile("^(?:\\d{9}|\\d{12})$");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PatientRepository patientRepository;

    public FileValidationServiceImpl(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Override
    public FileValidationResponse validate(ParsedFileData data, Long sourceSystemId) {
        List<ValidationErrorDto> errors = new ArrayList<>();
        List<PreviewRowDto> preview = new ArrayList<>();
        Set<String> localCodes = new HashSet<>();
        Set<String> nationalIds = new HashSet<>();

        Map<String, String> mappings = suggestMappings(data.headers());
        int validRows = 0;
        int invalidRows = 0;

        for (int index = 0; index < data.rows().size(); index++) {
            int rowNumber = index + 2;
            Map<String, String> row = data.rows().get(index);
            List<String> rowMessages = validateRow(row, mappings, sourceSystemId, localCodes, nationalIds);
            boolean valid = rowMessages.isEmpty();
            if (valid) {
                validRows++;
            } else {
                invalidRows++;
                rowMessages.forEach(message -> errors.add(
                        new ValidationErrorDto(rowNumber, "row", message, "ERROR")));
            }
            preview.add(new PreviewRowDto(
                    rowNumber,
                    new LinkedHashMap<>(row),
                    valid ? "VALID" : "INVALID",
                    rowMessages,
                    null));
        }

        return new FileValidationResponse(
                errors.isEmpty(),
                null,
                null,
                null,
                data.rows().size(),
                validRows,
                invalidRows,
                data.headers(),
                mappings,
                errors,
                preview);
    }

    private List<String> validateRow(
            Map<String, String> row,
            Map<String, String> mappings,
            Long sourceSystemId,
            Set<String> localCodes,
            Set<String> nationalIds) {
        List<String> messages = new ArrayList<>();
        String localCode = value(row, mappings, "localPatientCode");
        String fullName = value(row, mappings, "fullName");
        String nationalId = value(row, mappings, "nationalId");
        String dateOfBirth = value(row, mappings, "dateOfBirth");
        String phone = value(row, mappings, "phoneNumber");

        if (localCode.isBlank()) {
            messages.add("Thieu localPatientCode");
        } else if (!localCodes.add(localCode)) {
            messages.add("localPatientCode bi trung trong file");
        } else if (patientRepository.existsBySourceSystemIdAndLocalPatientCode(sourceSystemId, localCode)) {
            messages.add("localPatientCode da ton tai trong source system");
        }
        if (fullName.isBlank()) {
            messages.add("Thieu fullName");
        }
        if (!nationalId.isBlank()) {
            if (!NATIONAL_ID.matcher(nationalId).matches()) {
                messages.add("nationalId phai co 9 hoac 12 chu so");
            } else if (!nationalIds.add(nationalId)) {
                messages.add("nationalId bi trung trong file");
            }
        }
        if (!dateOfBirth.isBlank()) {
            try {
                LocalDate date = LocalDate.parse(dateOfBirth, DATE_FORMAT);
                if (date.isAfter(LocalDate.now())) {
                    messages.add("dateOfBirth khong duoc o tuong lai");
                }
            } catch (DateTimeParseException exception) {
                messages.add("dateOfBirth phai co dinh dang dd/MM/yyyy");
            }
        }
        if (!phone.isBlank() && !PHONE.matcher(phone).matches()) {
            messages.add("phoneNumber khong dung dinh dang");
        }
        return messages;
    }

    private Map<String, String> suggestMappings(List<String> headers) {
        Map<String, String> result = new LinkedHashMap<>();
        Map<String, List<String>> aliases = Map.of(
                "localPatientCode", List.of("localPatientCode", "local_code", "patient_code", "ma_benh_nhan"),
                "fullName", List.of("fullName", "full_name", "name", "ho_ten"),
                "dateOfBirth", List.of("dateOfBirth", "dob", "date_of_birth", "ngay_sinh"),
                "nationalId", List.of("nationalId", "national_id", "cccd", "cmnd"),
                "healthInsuranceNo", List.of("healthInsuranceNo", "health_insurance_no", "bhyt"),
                "phoneNumber", List.of("phoneNumber", "phone", "phone_number", "so_dien_thoai"),
                "gender", List.of("gender", "gioi_tinh"),
                "address", List.of("address", "dia_chi"));

        for (Map.Entry<String, List<String>> entry : aliases.entrySet()) {
            headers.stream()
                    .filter(header -> entry.getValue().stream().anyMatch(alias -> alias.equalsIgnoreCase(header)))
                    .findFirst()
                    .ifPresent(header -> result.put(entry.getKey(), header));
        }
        return result;
    }

    private String value(Map<String, String> row, Map<String, String> mappings, String field) {
        String header = mappings.get(field);
        return header == null || row.get(header) == null ? "" : row.get(header).trim();
    }
}
```

---

## 4. Async configuration

### `config/AsyncConfig.java`

```java
package com.mpi.demo.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "importTaskExecutor")
    public Executor importTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("mpi-import-");
        executor.initialize();
        return executor;
    }
}
```

### `service/AsyncImportProcessor.java`

```java
package com.mpi.demo.service;

public interface AsyncImportProcessor {
    void process(Long importJobId);
}
```

### `exception/ImportStateException.java`

```java
package com.mpi.demo.exception;

public class ImportStateException extends RuntimeException {
    public ImportStateException(String message) {
        super(message);
    }
}
```

### `service/impl/AsyncImportProcessorImpl.java`

```java
package com.mpi.demo.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mpi.demo.constant.GenderEnum;
import com.mpi.demo.constant.ImportJobStatusEnum;
import com.mpi.demo.constant.ImportRowStatusEnum;
import com.mpi.demo.constant.MatchDecisionEnum;
import com.mpi.demo.constant.MatchStatusEnum;
import com.mpi.demo.entity.ImportJob;
import com.mpi.demo.entity.ImportJobDetail;
import com.mpi.demo.entity.Patient;
import com.mpi.demo.entity.SourceSystem;
import com.mpi.demo.exception.ResourceNotFoundException;
import com.mpi.demo.repository.ImportJobDetailRepository;
import com.mpi.demo.repository.ImportJobRepository;
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
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ImportJobRepository importJobRepository;
    private final ImportJobDetailRepository detailRepository;
    private final PatientRepository patientRepository;
    private final SourceSystemRepository sourceSystemRepository;
    private final FileStorageService fileStorageService;
    private final FileParserService fileParserService;
    private final PatientMatchingService matchingService;
    private final PatientMasterService patientMasterService;

    public AsyncImportProcessorImpl(
            ImportJobRepository importJobRepository,
            ImportJobDetailRepository detailRepository,
            PatientRepository patientRepository,
            SourceSystemRepository sourceSystemRepository,
            FileStorageService fileStorageService,
            FileParserService fileParserService,
            PatientMatchingService matchingService,
            PatientMasterService patientMasterService) {
        this.importJobRepository = importJobRepository;
        this.detailRepository = detailRepository;
        this.patientRepository = patientRepository;
        this.sourceSystemRepository = sourceSystemRepository;
        this.fileStorageService = fileStorageService;
        this.fileParserService = fileParserService;
        this.matchingService = matchingService;
        this.patientMasterService = patientMasterService;
    }

    @Override
    @Async("importTaskExecutor")
    public void process(Long importJobId) {
        try {
            processInternal(importJobId);
        } catch (Exception exception) {
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
        StoredFile storedFile = fileStorageService.getMetadata(job.getFileName());
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
                detail.setErrorMessage("localPatientCode da ton tai");
                job.setDuplicateRows(job.getDuplicateRows() + 1);
            } else {
                Patient patient = toPatient(row, mappings, sourceSystem);
                patient = patientRepository.save(patient);
                List<com.mpi.demo.entity.MatchCandidate> candidates = matchingService.findMatchCandidates(patient);
                if (candidates.isEmpty()) {
                    patientMasterService.createMaster(patient);
                    detail.setStatus(ImportRowStatusEnum.SUCCESS);
                    job.setSuccessfulRows(job.getSuccessfulRows() + 1);
                } else {
                    com.mpi.demo.entity.MatchCandidate candidate = candidates.get(0);
                    detail.setMatchScore(candidate.getMatchScore());
                    detail.setMatchedMasterId(candidate.getCandidateMaster().getId());
                    if (candidate.getDecision() == MatchDecisionEnum.AUTO_APPROVED) {
                        patientMasterService.linkToMaster(patient.getId(), candidate.getCandidateMaster().getId());
                        detail.setStatus(ImportRowStatusEnum.SUCCESS);
                        job.setSuccessfulRows(job.getSuccessfulRows() + 1);
                    } else {
                        detail.setStatus(ImportRowStatusEnum.REQUIRES_REVIEW);
                        job.setWarningRows(job.getWarningRows() + 1);
                    }
                }
            }
        } catch (Exception exception) {
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
        return value.isBlank() ? null : LocalDate.parse(value, DATE_FORMAT);
    }

    private GenderEnum parseGender(String value) {
        if (value.isBlank()) return null;
        return switch (value.trim().toUpperCase()) {
            case "MALE", "NAM", "M" -> GenderEnum.MALE;
            case "FEMALE", "NU", "F" -> GenderEnum.FEMALE;
            case "OTHER", "KHAC" -> GenderEnum.OTHER;
            default -> GenderEnum.UNKNOWN;
        };
    }

    private Map<String, String> parseMappings(String configuration) {
        // Can thay bang ObjectMapper khi configuration duoc luu duoi dang JSON.
        return Map.of(
                "localPatientCode", "localPatientCode",
                "fullName", "fullName",
                "dateOfBirth", "dateOfBirth",
                "nationalId", "nationalId",
                "healthInsuranceNo", "healthInsuranceNo",
                "phoneNumber", "phoneNumber",
                "gender", "gender",
                "address", "address");
    }

    private String toJson(Map<String, String> row) {
        return row.toString();
    }
}
```

> Luu y quan trong: `ImportJob.fileName` dang duoc dung lam token trong code tren. Nen them field `fileToken` vao entity va thay `job.getFileName()` bang `job.getFileToken()` de tach ten hien thi va token luu tru.

---

## 5. Import service

### `service/ImportService.java`

```java
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
```

### `dto/response/ImportJobDetailResponse.java`

```java
package com.mpi.demo.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.mpi.demo.constant.ImportRowStatusEnum;
import com.mpi.demo.entity.ImportJobDetail;

public record ImportJobDetailResponse(
        Long id,
        Integer rowNumber,
        String rowData,
        ImportRowStatusEnum status,
        BigDecimal matchScore,
        Long patientId,
        Long matchedMasterId,
        String errorMessage,
        String warningMessage,
        LocalDateTime processedAt) {

    public static ImportJobDetailResponse fromEntity(ImportJobDetail detail) {
        return new ImportJobDetailResponse(
                detail.getId(),
                detail.getRowNumber(),
                detail.getRowData(),
                detail.getStatus(),
                detail.getMatchScore(),
                detail.getPatient() == null ? null : detail.getPatient().getId(),
                detail.getMatchedMasterId(),
                detail.getErrorMessage(),
                detail.getWarningMessage(),
                detail.getProcessedAt());
    }
}
```

### `service/impl/ImportServiceImpl.java`

```java
package com.mpi.demo.service.impl;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
import com.mpi.demo.exception.ImportStateException;
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
    public FileValidationResponse uploadAndValidateFile(UploadFileRequest request) {
        SourceSystem sourceSystem = sourceSystemRepository.findById(request.getSourceSystemId())
                .orElseThrow(() -> new ResourceNotFoundException("He thong nguon", "id", request.getSourceSystemId()));
        if (!Boolean.TRUE.equals(sourceSystem.getIsActive())) {
            throw new ImportStateException("He thong nguon dang bi vo hieu hoa");
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
            throw new ImportStateException("Khong the luu hoac doc file import");
        }
    }

    @Override
    public Page<PreviewRowDto> previewImportData(Long jobId, Pageable pageable) {
        ImportJob job = findJob(jobId);
        StoredFile storedFile = fileStorageService.getMetadata(job.getFileName());
        try {
            ParsedFileData parsed = fileParserService.parse(storedFile.path(), job.getFileType());
            FileValidationResponse validation = validationService.validate(parsed, job.getSourceSystem().getId());
            int start = Math.toIntExact(pageable.getOffset());
            int end = Math.min(start + pageable.getPageSize(), validation.preview().size());
            List<PreviewRowDto> content = start >= validation.preview().size()
                    ? List.of()
                    : validation.preview().subList(start, end);
            return new org.springframework.data.domain.PageImpl<>(content, pageable, validation.preview().size());
        } catch (IOException exception) {
            throw new ImportStateException("Khong the doc file preview");
        }
    }

    @Override
    @Transactional
    public ImportJobResponse startImport(StartImportRequest request) {
        SourceSystem sourceSystem = sourceSystemRepository.findById(request.getSourceSystemId())
                .orElseThrow(() -> new ResourceNotFoundException("He thong nguon", "id", request.getSourceSystemId()));
        StoredFile storedFile = fileStorageService.getMetadata(request.getFileToken());
        ImportJob job = ImportJob.builder()
                .fileName(request.getFileToken())
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
                .createdBy(0L)
                .build();
        ImportJob saved = importJobRepository.save(job);
        asyncImportProcessor.process(saved.getId());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ImportJobResponse getImportJobStatus(Long jobId) {
        return toResponse(findJob(jobId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ImportJobDetailResponse> getImportJobDetails(
            Long jobId, ImportRowStatusEnum status, Pageable pageable) {
        findJob(jobId);
        Page<ImportJobDetail> page = status == null
                ? detailRepository.findByImportJobIdOrderByRowNumber(jobId, pageable)
                : detailRepository.findByImportJobIdAndStatus(jobId, status, pageable);
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
            page = importJobRepository.findAll(pageable);
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
            throw new ImportStateException("Job khong con o trang thai co the huy");
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
            throw new ImportStateException("Chi duoc retry job da ket thuc");
        }
        job.setStatus(ImportJobStatusEnum.PENDING);
        job.setErrorMessage(null);
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
                job.getId(), job.getFileName(), job.getFileSize(), job.getFileType(),
                job.getSourceSystem().getId(), job.getSourceSystem().getName(), job.getStatus(),
                job.getTotalRows(), job.getProcessedRows(), job.getSuccessfulRows(), job.getFailedRows(),
                job.getDuplicateRows(), job.getWarningRows(), progress, job.getStartedAt(), job.getCompletedAt(),
                job.getErrorMessage(), job.getCreatedBy(), null, job.getCreatedAt());
    }

    private String toJson(Map<String, String> mappings) {
        try {
            return objectMapper.writeValueAsString(mappings);
        } catch (JsonProcessingException exception) {
            throw new ImportStateException("Khong the luu cau hinh mapping");
        }
    }
}
```

> `createdBy(0L)` chi la gia tri tam thoi do project hien tai chua co SecurityContext/User service. Khi co authentication, thay bang id user dang dang nhap.

---

## 6. Import controller

### `controller/ImportController.java`

```java
package com.mpi.demo.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ModelAttribute;

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

import jakarta.validation.Valid;

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
        return ResponseEntity.ok(ApiResponse.success(
                "Upload va validate file thanh cong", importService.uploadAndValidateFile(request)));
    }

    @GetMapping("/{jobId}/preview")
    public ResponseEntity<ApiResponse<Page<PreviewRowDto>>> preview(
            @PathVariable Long jobId,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lay preview import thanh cong", importService.previewImportData(jobId, pageable)));
    }

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<ImportJobResponse>> start(
            @Valid @RequestBody StartImportRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Da khoi dong import", importService.startImport(request)));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<ApiResponse<ImportJobResponse>> status(@PathVariable Long jobId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lay trang thai import thanh cong", importService.getImportJobStatus(jobId)));
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
```

---

## 7. Enum va repository can thay doi

### `constant/ImportRowStatusEnum.java`

```java
package com.mpi.demo.constant;

public enum ImportRowStatusEnum {
    PENDING,
    SUCCESS,
    FAILED,
    DUPLICATE,
    WARNING,
    REQUIRES_REVIEW
}
```

### Bo sung vao `ImportJob.java`

Them field sau vao entity hien tai:

```java
@Column(name = "file_token", nullable = false, unique = true, length = 100)
private String fileToken;
```

Sua builder trong `ImportServiceImpl`:

```java
.fileToken(request.getFileToken())
.fileName(storedFile.originalFileName())
```

Sua processor:

```java
StoredFile storedFile = fileStorageService.getMetadata(job.getFileToken());
```

`length = 100` la do dai luu token UUID va co the giu nguyen khi copy vao Java.

### Bo sung getter query vao `ImportJobDetailRepository.java`

```java
Page<ImportJobDetail> findByImportJobIdAndStatus(
        Long importJobId,
        ImportRowStatusEnum status,
        Pageable pageable);

Page<ImportJobDetail> findByImportJobIdOrderByRowNumber(
        Long importJobId,
        Pageable pageable);
```

Neu hai method nay da ton tai thi giu nguyen, khong tao trung.

---

## 8. `application.yaml`

Them vao file hien tai:

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 500MB
      max-request-size: 500MB

file:
  upload:
    dir: ${MPI_UPLOAD_DIR:./uploads/imports}
    max-size: ${MPI_UPLOAD_MAX_SIZE:524288000}

import:
  max-rows: ${MPI_IMPORT_MAX_ROWS:500000}
  cleanup-after-completion: ${MPI_IMPORT_CLEANUP:true}
```

Khong hard-code mat khau database trong production. Dung:

```yaml
spring:
  datasource:
    url: ${MPI_DB_URL}
    username: ${MPI_DB_USERNAME}
    password: ${MPI_DB_PASSWORD}
```

---

## 9. `pom.xml`

Project da co POI va OpenCSV. Can dam bao co cac dependency sau trong `<dependencies>`:

```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>

<dependency>
    <groupId>com.opencsv</groupId>
    <artifactId>opencsv</artifactId>
    <version>5.9</version>
</dependency>

<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

Khong them lai dependency neu da co trong `pom.xml`.

---

## 10. Cac diem can sua truoc khi copy code vao project

1. Them `fileToken` vao `ImportJob`; khong nen dung `fileName` de lam token.
2. `AsyncImportProcessorImpl.parseMappings()` phai dung `ObjectMapper.readValue(configuration, ...)` de doc mapping da luu.
3. Thay `createdBy(0L)` bang user id tu security context sau khi co authentication.
4. `ImportJobDetail.rowData` nen luu JSON that, khong dung `Map.toString()` neu can doc lai du lieu.
5. `PatientMatchingService.findMatchCandidates()` hien tai chi tao object candidate; neu muon review queue, can `matchCandidateRepository.saveAll(candidates)` trong service matching.
6. Neu `MatchDecisionEnum` khong co `AUTO_APPROVED` hoac `PENDING` dung nhu code tren, doi ten theo enum hien tai.
7. Neu `GenderEnum` khong co `UNKNOWN`, bo nhanh `GenderEnum.UNKNOWN` hoac bo sung enum.
8. Them `@ExceptionHandler` cho `FileStorageException`, `FileParsingException`, `ImportStateException` vao `GlobalExceptionHandler`.
9. Khong tra JPA entity truc tiep tu controller; code tren da dung `ImportJobResponse` va `ImportJobDetailResponse`.
10. Chay compile/test sau khi copy tung nhom file; khong copy toan bo mot lan khi chua kiem tra cac contract enum/entity hien tai.

## 11. Exception handler bo sung

Them cac method sau vao `GlobalExceptionHandler`:

```java
@ExceptionHandler({
        FileStorageException.class,
        FileParsingException.class,
        ImportStateException.class
})
public ResponseEntity<ApiResponse<Void>> handleImportException(RuntimeException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.badRequest(exception.getMessage()));
}
```

Them import:

```java
import com.mpi.demo.exception.FileParsingException;
import com.mpi.demo.exception.FileStorageException;
import com.mpi.demo.exception.ImportStateException;
```

## 12. Checklist sau khi ap dung

- [ ] Chi tao/sua code sau khi da doc tung block trong tai lieu.
- [ ] `ImportServiceImpl implements ImportService`.
- [ ] Upload tao token UUID, khong dung ten file lam path.
- [ ] CSV BOM va XLSX deu parse duoc.
- [ ] Header trung/empty bi tu choi.
- [ ] Validate duplicate local code, national ID, DOB, phone.
- [ ] Start job tao `ImportJob` va goi async processor.
- [ ] Processor cap nhat detail/counters va xu ly cancel.
- [ ] Controller co upload, preview, start, status, details, search, cancel, retry.
- [ ] Enum row status co day du gia tri service su dung.
- [ ] Entity co `fileToken` rieng voi `fileName`.
- [ ] Khong sua truc tiep code goc ngoai cac file ban muon ap dung.
