# 📊 Backend Implementation Status & Action Plan

## 🎯 Tổng quan hệ thống MPI (Master Patient Index)

### Mục tiêu

Xây dựng hệ thống MPI để:

- Quản lý thông tin bệnh nhân từ nhiều nguồn (source systems)
- Import dữ liệu bệnh nhân hàng loạt (CSV/Excel)
- Matching & deduplication tự động
- Tạo Enterprise MPI ID duy nhất cho mỗi bệnh nhân

---

## ✅ ĐÃ HOÀN THÀNH (Completed)

### 1. **Core Entities** ✅ (8/8)


| Entity            | Status | Description                      |
| ----------------- | ------ | -------------------------------- |
| `Patient`         | ✅      | Bệnh nhân từ từng source system  |
| `PatientMaster`   | ✅      | Golden record - EMPI master      |
| `Facility`        | ✅      | Cơ sở y tế                       |
| `SourceSystem`    | ✅      | Hệ thống nguồn (HIS, EMR, etc)   |
| `User`            | ✅      | Người dùng hệ thống              |
| `MatchCandidate`  | ✅      | Ứng viên match cho manual review |
| `ImportJob`       | ✅      | Job import file                  |
| `ImportJobDetail` | ✅      | Chi tiết từng row import         |




### 2. **Enums/Constants** ✅ (9/9)

```
✅ GenderEnum
✅ PatientStatusEnum
✅ FacilityTypeEnum
✅ MatchStatusEnum
✅ MatchDecisionEnum
✅ FileTypeEnum (CSV, EXCEL)
✅ ImportJobStatusEnum (PENDING, VALIDATING, PROCESSING, COMPLETED, FAILED)
✅ ImportRowStatusEnum (PENDING, SUCCESS, FAILED, DUPLICATE, WARNING, REQUIRES_REVIEW)
```



### 3. **Repositories** ✅ (7/7)


| Repository                  | Queries | Pagination | Notes                                   |
| --------------------------- | ------- | ---------- | --------------------------------------- |
| `PatientRepository`         | ✅       | ✅ Hybrid   | Page cho search, Optional cho lookup    |
| `PatientMasterRepository`   | ✅       | ❌ List     | 4 blocking passes cho matching          |
| `FacilityRepository`        | ✅       | ✅ Hybrid   | Page cho search, Optional cho lookup    |
| `SourceSystemRepository`    | ✅       | ✅ Hybrid   | Page cho search, Optional cho lookup    |
| `MatchCandidateRepository`  | ✅       | ✅ Hybrid   | Page cho review queue, List cho patient |
| `ImportJobRepository`       | ✅       | ✅ All Page | Admin UI listing                        |
| `ImportJobDetailRepository` | ✅       | ✅ All Page | Detail rows có thể 10k+                 |


**✅ Đã tối ưu phân trang đúng chuẩn production!**

### 4. **Basic CRUD Services** ✅ (3/3)

```
✅ FacilityService + FacilityServiceImpl
✅ SourceSystemService + SourceSystemServiceImpl
✅ PatientService + PatientServiceImpl
```



### 5. **Controllers** ✅ (3/3)

```
✅ FacilityController - CRUD cơ sở y tế
✅ SourceSystemController - CRUD hệ thống nguồn
✅ PatientController - CRUD bệnh nhân
```



### 6. **DTOs** ✅ (19/19)

**Request DTOs:**

```
✅ CreateFacilityRequest, UpdateFacilityRequest, FacilitySearchRequest
✅ CreateSourceSystemRequest, UpdateSourceSystemRequest, SourceSystemSearchRequest
✅ CreatePatientRequest, UpdatePatientRequest, PatientSearchRequest
✅ UploadFileRequest
✅ StartImportRequest
✅ ImportJobSearchRequest
```

**Response DTOs:**

```
✅ FacilityResponse
✅ SourceSystemResponse
✅ PatientResponse
✅ ImportJobResponse
✅ FileValidationResponse
✅ ValidationErrorDto
✅ PreviewRowDto
```



### 7. **Infrastructure** ✅

```
✅ CorsConfig
✅ GlobalExceptionHandler
✅ ApiResponse helper
✅ ResultPagination helper
✅ JPA Specifications (Facility, SourceSystem, Patient)
```



### 8. **Dependencies** ✅

```xml
✅ Spring Boot 4.0.7
✅ Spring Data JPA
✅ Spring Validation
✅ MySQL Connector
✅ Lombok
✅ SpringDoc OpenAPI (Swagger)
✅ Apache POI 5.2.5 (Excel processing)
✅ OpenCSV 5.9 (CSV processing)
✅ Apache Commons Text 1.11.0 (String similarity)
```

---



## ❌ CHƯA LÀM (Missing - Cần làm ngay)



### 🔴 **CRITICAL - Core Business Logic**



#### 1. **Import Service** ❌ THIẾU HOÀN TOÀN

**Cần tạo:**

**a)** `ImportService.java` **(Interface)**

```java
package com.mpi.demo.service;

public interface ImportService {
    // Step 1: Upload & validate file
    FileValidationResponse uploadAndValidateFile(UploadFileRequest request);
    
    // Step 2: Preview trước khi import
    ResultPagination<PreviewRowDto> previewImportData(Long jobId, Pageable pageable);
    
    // Step 3: Start import process
    ImportJobResponse startImport(StartImportRequest request);
    
    // Step 4: Get import job status
    ImportJobResponse getImportJobStatus(Long jobId);
    
    // Step 5: Get import job details
    ResultPagination<ImportJobDetail> getImportJobDetails(Long jobId, ImportRowStatusEnum status, Pageable pageable);
    
    // Management
    ResultPagination<ImportJobResponse> searchImportJobs(ImportJobSearchRequest request, Pageable pageable);
    void cancelImportJob(Long jobId);
    void retryFailedRows(Long jobId);
}
```

**b)** `ImportServiceImpl.java` **(Implementation)**

```java
package com.mpi.demo.service.impl;

@Service
public class ImportServiceImpl implements ImportService {
    
    @Autowired
    private ImportJobRepository importJobRepository;
    
    @Autowired
    private ImportJobDetailRepository importJobDetailRepository;
    
    @Autowired
    private FileParserService fileParserService;
    
    @Autowired
    private FileValidationService fileValidationService;
    
    @Autowired
    private PatientMatchingService patientMatchingService;
    
    @Autowired
    private PatientMasterService patientMasterService;
    
    // Implement 7 methods above
}
```

**File cần tạo:**

```
✅ service/ImportService.java
✅ service/impl/ImportServiceImpl.java
```

---



#### 2. **File Processing Services** ❌ THIẾU HOÀN TOÀN

**a)** `FileParserService.java`

```java
package com.mpi.demo.service;

public interface FileParserService {
    List<Map<String, String>> parseFile(MultipartFile file, FileTypeEnum fileType);
    List<String> getHeaders(MultipartFile file, FileTypeEnum fileType);
    boolean validateFileFormat(MultipartFile file, FileTypeEnum fileType);
}
```

**b)** `FileValidationService.java`

```java
package com.mpi.demo.service;

public interface FileValidationService {
    FileValidationResponse validateImportData(List<Map<String, String>> rows, Long sourceSystemId);
    List<ValidationErrorDto> validateRow(Map<String, String> row, int rowNumber);
    boolean isValidNationalId(String nationalId);
    boolean isValidPhoneNumber(String phoneNumber);
    boolean isValidDate(String dateStr);
}
```

**File cần tạo:**

```
✅ service/FileParserService.java
✅ service/impl/FileParserServiceImpl.java
✅ service/FileValidationService.java
✅ service/impl/FileValidationServiceImpl.java
```

---



#### 3. **Patient Matching Service** ❌ THIẾU HOÀN TOÀN - QUAN TRỌNG NHẤT!

**a)** `PatientMatchingService.java`

```java
package com.mpi.demo.service;

public interface PatientMatchingService {
    // Core matching algorithm - 4 blocking passes
    List<MatchCandidate> findMatchCandidates(Patient patient);
    
    // Calculate match score
    BigDecimal calculateMatchScore(Patient patient, PatientMaster master);
    
    // Auto-decision rules
    MatchDecisionEnum getAutoDecision(BigDecimal matchScore);
    
    // Process match decision
    void processMatchDecision(Long candidateId, MatchDecisionEnum decision, Long reviewerId);
    
    // Get pending reviews
    ResultPagination<MatchCandidate> getPendingReviews(Pageable pageable);
}
```

**b)** `PatientMatchingServiceImpl.java`

```java
@Service
public class PatientMatchingServiceImpl implements PatientMatchingService {
    
    // BLOCKING STRATEGY - 4 PASSES
    @Override
    public List<MatchCandidate> findMatchCandidates(Patient patient) {
        Set<PatientMaster> candidates = new HashSet<>();
        
        // Pass 1: DOB + First 3 chars of name
        if (patient.getDateOfBirth() != null) {
            String first3 = patient.getFullName().substring(0, 3).toUpperCase();
            candidates.addAll(
                patientMasterRepo.findByDateOfBirthAndFirst3Chars(
                    patient.getDateOfBirth(), first3
                )
            );
        }
        
        // Pass 2: Last 4 digits of phone
        if (patient.getPhoneNumber() != null && patient.getPhoneNumber().length() >= 4) {
            String last4 = patient.getPhoneNumber().substring(
                patient.getPhoneNumber().length() - 4
            );
            candidates.addAll(
                patientMasterRepo.findByPhoneLast4Digits(last4)
            );
        }
        
        // Pass 3: Soundex/normalized name
        String normalized = normalizeName(patient.getFullName());
        candidates.addAll(
            patientMasterRepo.findByNameSoundex(normalized)
        );
        
        // Pass 4: Exact National ID (if available)
        if (patient.getNationalId() != null) {
            patientMasterRepo.findByNationalId(patient.getNationalId())
                .ifPresent(candidates::add);
        }
        
        // Calculate scores for each candidate
        return candidates.stream()
            .map(master -> buildMatchCandidate(patient, master))
            .filter(mc -> mc.getMatchScore().compareTo(new BigDecimal("30.00")) >= 0)
            .sorted((a, b) -> b.getMatchScore().compareTo(a.getMatchScore()))
            .collect(Collectors.toList());
    }
    
    @Override
    public BigDecimal calculateMatchScore(Patient patient, PatientMaster master) {
        // WEIGHTED SCORING ALGORITHM
        BigDecimal score = BigDecimal.ZERO;
        
        // National ID: 40 points (exact match)
        if (isExactMatch(patient.getNationalId(), master.getNationalId())) {
            score = score.add(new BigDecimal("40.00"));
        }
        
        // Full Name: 25 points (Levenshtein similarity)
        double nameSim = calculateStringSimilarity(patient.getFullName(), master.getFullName());
        score = score.add(new BigDecimal(nameSim * 25));
        
        // Date of Birth: 20 points (exact match)
        if (patient.getDateOfBirth() != null && 
            patient.getDateOfBirth().equals(master.getDateOfBirth())) {
            score = score.add(new BigDecimal("20.00"));
        }
        
        // Phone: 10 points (exact match)
        if (isExactMatch(patient.getPhoneNumber(), master.getPhoneNumber())) {
            score = score.add(new BigDecimal("10.00"));
        }
        
        // Gender: 5 points (exact match)
        if (patient.getGender() == master.getGender()) {
            score = score.add(new BigDecimal("5.00"));
        }
        
        return score.setScale(2, RoundingMode.HALF_UP);
    }
    
    @Override
    public MatchDecisionEnum getAutoDecision(BigDecimal matchScore) {
        // DECISION RULES
        if (matchScore.compareTo(new BigDecimal("85.00")) >= 0) {
            return MatchDecisionEnum.AUTO_APPROVED; // 85+ = auto merge
        }
        if (matchScore.compareTo(new BigDecimal("50.00")) >= 0) {
            return MatchDecisionEnum.PENDING; // 50-84 = manual review
        }
        return MatchDecisionEnum.REJECTED; // <50 = auto reject
    }
}
```

**File cần tạo:**

```
✅ service/PatientMatchingService.java
✅ service/impl/PatientMatchingServiceImpl.java
✅ util/StringSimilarityUtil.java (Levenshtein distance)
✅ util/NameNormalizationUtil.java (Vietnamese text normalization)
```

---



#### 4. **Patient Master Service** ❌ THIẾU HOÀN TOÀN

**a)** `PatientMasterService.java`

```java
package com.mpi.demo.service;

public interface PatientMasterService {
    // Create new master record
    PatientMaster createMaster(Patient patient);
    
    // Link patient to existing master
    void linkToMaster(Long patientId, Long masterId);
    
    // Merge two masters
    PatientMaster mergeMasters(Long sourceId, Long targetId, Long userId);
    
    // Unlink patient from master
    void unlinkPatient(Long patientId);
    
    // Generate enterprise ID
    String generateEnterpriseId();
    
    // Search masters
    ResultPagination<PatientMaster> searchMasters(String keyword, Pageable pageable);
}
```

**File cần tạo:**

```
✅ service/PatientMasterService.java
✅ service/impl/PatientMasterServiceImpl.java
```

---



#### 5. **Controllers cho Import & Matching** ❌ THIẾU HOÀN TOÀN

**a)** `ImportController.java`

```java
@RestController
@RequestMapping("/api/v1/imports")
public class ImportController {
    
    @PostMapping("/upload")
    public ResponseEntity<FileValidationResponse> uploadFile(@RequestBody UploadFileRequest request);
    
    @GetMapping("/{jobId}/preview")
    public ResponseEntity<ResultPagination<PreviewRowDto>> previewImport(@PathVariable Long jobId);
    
    @PostMapping("/start")
    public ResponseEntity<ImportJobResponse> startImport(@RequestBody StartImportRequest request);
    
    @GetMapping("/{jobId}/status")
    public ResponseEntity<ImportJobResponse> getJobStatus(@PathVariable Long jobId);
    
    @GetMapping("/{jobId}/details")
    public ResponseEntity<ResultPagination<ImportJobDetail>> getJobDetails(@PathVariable Long jobId);
    
    @GetMapping
    public ResponseEntity<ResultPagination<ImportJobResponse>> searchJobs();
    
    @PostMapping("/{jobId}/cancel")
    public ResponseEntity<Void> cancelJob(@PathVariable Long jobId);
    
    @PostMapping("/{jobId}/retry")
    public ResponseEntity<Void> retryFailedRows(@PathVariable Long jobId);
}
```

**b)** `MatchReviewController.java`

```java
@RestController
@RequestMapping("/api/v1/match-reviews")
public class MatchReviewController {
    
    @GetMapping("/pending")
    public ResponseEntity<ResultPagination<MatchCandidate>> getPendingReviews();
    
    @PostMapping("/{candidateId}/approve")
    public ResponseEntity<Void> approveMatch(@PathVariable Long candidateId);
    
    @PostMapping("/{candidateId}/reject")
    public ResponseEntity<Void> rejectMatch(@PathVariable Long candidateId);
    
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<MatchCandidate>> getCandidatesForPatient(@PathVariable Long patientId);
}
```

**c)** `PatientMasterController.java`

```java
@RestController
@RequestMapping("/api/v1/patient-masters")
public class PatientMasterController {
    
    @GetMapping
    public ResponseEntity<ResultPagination<PatientMaster>> searchMasters();
    
    @GetMapping("/{id}")
    public ResponseEntity<PatientMaster> getMaster(@PathVariable Long id);
    
    @PostMapping("/merge")
    public ResponseEntity<PatientMaster> mergeMasters(@RequestBody MergeMasterRequest request);
    
    @PostMapping("/{masterId}/unlink/{patientId}")
    public ResponseEntity<Void> unlinkPatient(@PathVariable Long masterId, @PathVariable Long patientId);
}
```

**File cần tạo:**

```
✅ controller/ImportController.java
✅ controller/MatchReviewController.java
✅ controller/PatientMasterController.java
```

---



### 🟡 **OPTIONAL - Enhancements**



#### 6. **Authentication & Authorization** (Tùy chọn - có thể làm sau)

```
❌ UserRepository
❌ RoleRepository (nếu có RBAC)
❌ UserService
❌ AuthService
❌ SecurityConfig
❌ JwtTokenProvider (nếu dùng JWT)
❌ AuthController (login, register, refresh token)
```

**Nếu làm đơn giản:**

- Dùng Spring Security với in-memory users
- Hoặc bỏ qua auth trong giai đoạn đầu



#### 7. **Async Processing** (Recommended cho import lớn)

```
❌ @Async configuration
❌ AsyncImportProcessor - xử lý import async
❌ WebSocket notification cho real-time progress
```



#### 8. **Audit Trail**

```
❌ AuditLog entity
❌ AuditLogRepository
❌ @EntityListeners cho auto audit
```



#### 9. **Caching** (Optional)

```
❌ Redis config
❌ Cache cho master lookup
```

---



## 📝 ACTION PLAN - THỨ TỰ LÀM



### Phase 1: Core Matching Logic (1-2 ngày) 🔴 QUAN TRỌNG NHẤT

```
1️⃣ StringSimilarityUtil.java - Levenshtein distance
2️⃣ NameNormalizationUtil.java - Vietnamese text normalization
3️⃣ PatientMasterService + Impl
4️⃣ PatientMatchingService + Impl (4 blocking passes + scoring)
5️⃣ PatientMasterController
6️⃣ MatchReviewController
```

**Test ngay:**

- Tạo vài patient masters thủ công
- Import patient mới → check matching có hoạt động không

---



### Phase 2: File Import Flow (1-2 ngày)

```
7️⃣ FileParserService + Impl (CSV/Excel parsing)
8️⃣ FileValidationService + Impl
9️⃣ ImportService + Impl (orchestrate toàn bộ flow)
🔟 ImportController
```

**Test:**

- Upload CSV/Excel → validate → preview → import
- Check ImportJob + ImportJobDetail records
- Check matching được trigger

---



### Phase 3: Polish & Production Ready (1 ngày)

```
1️⃣1️⃣ Error handling improvements
1️⃣2️⃣ API documentation (Swagger)
1️⃣3️⃣ Logging
1️⃣4️⃣ Performance optimization (index, query tuning)
1️⃣5️⃣ Integration tests
```

---



### Phase 4: Optional Features (Nếu có thời gian)

```
1️⃣6️⃣ Authentication & Authorization
1️⃣7️⃣ Async processing cho import lớn
1️⃣8️⃣ WebSocket real-time progress
1️⃣9️⃣ Audit trail
2️⃣0️⃣ Caching
```

---



## 📊 SUMMARY



### Đã làm: 65%

```
✅ Database schema (entities) - 100%
✅ Repositories - 100%
✅ Basic CRUD - 100% (3 modules)
✅ DTOs - 100%
✅ Infrastructure - 100%
```



### Còn thiếu: 35% (CRITICAL)

```
❌ Matching algorithm - 0% 🔴 QUAN TRỌNG NHẤT
❌ Import processing - 0% 🔴
❌ File parsing - 0% 🔴
❌ 3 controllers mới - 0% 🔴
```

---



## 🎯 NEXT IMMEDIATE STEPS

1. **Tạo StringSimilarityUtil** → test Levenshtein
2. **Tạo PatientMatchingService** → implement 4 blocking passes
3. **Test matching thủ công** → verify algorithm
4. **Tạo FileParserService** → parse CSV/Excel
5. **Tạo ImportService** → orchestrate flow
6. **Test end-to-end** → upload file → match → review

---



## 📁 FILE STRUCTURE CẦN TẠO

```
be/mpi/demo/src/main/java/com/mpi/demo/
├── controller/
│   ├── ImportController.java ❌
│   ├── MatchReviewController.java ❌
│   └── PatientMasterController.java ❌
├── service/
│   ├── ImportService.java ❌
│   ├── FileParserService.java ❌
│   ├── FileValidationService.java ❌
│   ├── PatientMatchingService.java ❌
│   └── PatientMasterService.java ❌
├── service/impl/
│   ├── ImportServiceImpl.java ❌
│   ├── FileParserServiceImpl.java ❌
│   ├── FileValidationServiceImpl.java ❌
│   ├── PatientMatchingServiceImpl.java ❌
│   └── PatientMasterServiceImpl.java ❌
├── util/
│   ├── StringSimilarityUtil.java ❌
│   └── NameNormalizationUtil.java ❌
└── dto/
    ├── request/
    │   └── MergeMasterRequest.java ❌
    └── response/
        ├── MatchCandidateResponse.java ❌
        └── PatientMasterResponse.java ❌
```

**Tổng cộng cần tạo: ~20 files**

---



## 💡 RECOMMENDATIONS



### 1. Database Indexes (Quan trọng cho performance)

```sql
-- Thêm indexes cho matching queries
CREATE INDEX idx_patient_master_dob ON patient_master(date_of_birth);
CREATE INDEX idx_patient_master_national_id ON patient_master(national_id);
CREATE INDEX idx_patient_master_phone ON patient_master(phone_number);
CREATE INDEX idx_patient_master_name ON patient_master(full_name);
CREATE INDEX idx_patient_master_status ON patient_master(status);

-- Indexes cho import
CREATE INDEX idx_import_job_status ON import_job(status);
CREATE INDEX idx_import_job_created_by ON import_job(created_by);
CREATE INDEX idx_import_job_detail_status ON import_job_detail(status);
CREATE INDEX idx_import_job_detail_job_id ON import_job_detail(import_job_id);

-- Indexes cho matching
CREATE INDEX idx_match_candidate_decision ON match_candidate(decision);
CREATE INDEX idx_match_candidate_patient_id ON match_candidate(patient_id);
```



### 2. Application Properties

```yaml
# application.yaml additions
spring:
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB
      
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 50
        order_inserts: true
        order_updates: true

# Custom properties
mpi:
  import:
    batch-size: 500
    max-file-size: 52428800 # 50MB
    allowed-extensions: csv,xlsx,xls
  matching:
    auto-approve-threshold: 85.00
    manual-review-threshold: 50.00
    max-candidates: 10
  enterprise-id:
    prefix: EMPI
    year-format: yyyy
    sequence-length: 6
```



### 3. Logging Strategy

```java
// Thêm vào mỗi service
private static final Logger log = LoggerFactory.getLogger(ClassName.class);

// Log critical operations
log.info("Starting import job: jobId={}, fileName={}", jobId, fileName);
log.warn("Match candidate requires review: patientId={}, score={}", patientId, score);
log.error("Import failed: jobId={}, error={}", jobId, e.getMessage(), e);
```

---



## 🚀 KẾT LUẬN

**Backend đã có foundation tốt (65%)**, nhưng còn thiếu **core business logic (35%)**:

### Critical Missing:

1. **Matching algorithm** - Trái tim của hệ thống MPI
2. **Import processing** - Flow chính của user
3. **File parsing** - Input mechanism



### Ưu tiên tuyệt đối:

1. **PatientMatchingService** - Làm trước tiên
2. **ImportService** - Flow hoàn chỉnh
3. **Controllers** - API endpoints

**Estimate: 3-5 ngày** để hoàn thiện core features và test thoroughly.

---

📅 **Created:** 2026-09-21  
👨‍💻 **Status:** Ready for Phase 1 implementation  
🎯 **Focus:** Matching Algorithm → Import Flow → Polish