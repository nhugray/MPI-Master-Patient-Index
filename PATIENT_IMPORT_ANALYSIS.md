# Phân Tích Chi Tiết: Trang Import Dữ Liệu Bệnh Nhân

> **Document Version:** 2.0 (Đã cập nhật sau review feedback)  
> **Last Updated:** Sep 14, 2026  
> **Status:** ✅ Ready for Implementation

---

## 🎯 TÓM TẮT CÁC THAY ĐỔI QUAN TRỌNG

### ✅ BẮT BUỘC - Đã Fix

**1. Bug Fix: Gọi** `saveMatchCandidate()` **trong AsyncImportProcessor**

- **Vấn đề:** Không lưu match candidates khi phát hiện duplicate
- **Fix:** Thêm call sau khi detect duplicate (xem Section 4.6)
- **Impact:** Track được tất cả duplicates trong `match_candidate` table

**2. Nghiệp Vụ: Thêm Veto Rule giới tính**

- **Vấn đề:** Match sai người khi cùng tên + DOB nhưng khác giới tính
- **Fix:** Return 0 nếu gender mismatch (xem Section 4.5)
- **Impact:** Tránh false positive

**3. Security: Đổi** `DATA_MANAGER` **→** `REVIEWER`

- **Vấn đề:** Role name không khớp database
- **Fix:** `@PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")` (xem Section 8.2)
- **Impact:** Authorization hoạt động đúng

**4. Compile Error: Đổi** `RowStatus` **→** `ImportRowStatus`

- **Vấn đề:** Enum name sai
- **Fix:** `ImportRowStatus.FAILED` (xem Section 6.3)
- **Impact:** Code compile được



### 🚀 CẢI TIẾN - Đã Implement

**5. Weight Redistribution**

- Fair scoring khi patient thiếu field (xem Section 4.5)
- Example: Thiếu CMND (20 điểm) → scale 65/80 = 81.25%

**6. Blocking Strategy**

- Tối ưu duplicate detection (xem Section 4.5)
- Giảm search space: 100,000 → 100-500 records



### 🤔 QUYẾT ĐỊNH THIẾT KẾ

**7. ✅ BỎ Adaptive Weight Learning trong Phase 1**

- Dùng fixed weights, không cần bảng `weight_config`
- Simple is better

**8. ✅ KHUYẾN NGHỊ: Dùng Polling thay WebSocket**

- Đơn giản, dễ debug, đủ tốt cho import jobs
- Có thể upgrade sau

---



## 📋 Mục Lục

1. [Tổng Quan Chức Năng](#1-tổng-quan-chức-năng)
2. [Kiến Trúc Hệ Thống](#2-kiến-trúc-hệ-thống)
3. [Frontend Implementation](#3-frontend-implementation)
4. [Backend Implementation](#4-backend-implementation)
5. [Flow Xử Lý Import](#5-flow-xử-lý-import)
6. [Validation & Error Handling](#6-validation--error-handling)
7. [Performance & Scalability](#7-performance--scalability)
8. [Security Considerations](#8-security-considerations)

---



## 1. Tổng Quan Chức Năng



### 1.1 Mục đích

Cho phép người dùng import hàng loạt dữ liệu bệnh nhân từ file CSV hoặc XLSX vào hệ thống MPI (Master Patient Index), với các tính năng:

- Upload file (drag & drop hoặc chọn file)
- Preview dữ liệu trước khi import
- Validation dữ liệu real-time
- Mapping tự động các cột
- Xử lý duplicate detection
- Hiển thị tiến độ import
- Queue management cho multiple imports



### 1.2 Use Cases

- **Import Initial Data**: Load dữ liệu lịch sử từ hệ thống cũ
- **Periodic Sync**: Đồng bộ định kỳ từ các nguồn external
- **Data Migration**: Chuyển đổi dữ liệu giữa các hệ thống
- **Bulk Update**: Cập nhật hàng loạt thông tin bệnh nhân

---



## 2. Kiến Trúc Hệ Thống



### 2.1 Component Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     FRONTEND LAYER                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────┐  ┌──────────────────┐               │
│  │  Import Page     │  │  Upload Service  │               │
│  │  Component       │──│  (File Handling) │               │
│  └──────────────────┘  └──────────────────┘               │
│           │                      │                          │
│           │                      │                          │
│  ┌──────────────────┐  ┌──────────────────┐               │
│  │  Preview         │  │  Import Job      │               │
│  │  Component       │  │  Service         │               │
│  └──────────────────┘  └──────────────────┘               │
│           │                      │                          │
└───────────┼──────────────────────┼──────────────────────────┘
            │                      │
            │   REST API Calls     │
            ▼                      ▼
┌─────────────────────────────────────────────────────────────┐
│                     BACKEND LAYER                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────┐  ┌──────────────────┐               │
│  │  Import          │  │  File Parser     │               │
│  │  Controller      │──│  Service         │               │
│  └──────────────────┘  └──────────────────┘               │
│           │                      │                          │
│           │                      │                          │
│  ┌──────────────────┐  ┌──────────────────┐               │
│  │  Validation      │  │  Async Job       │               │
│  │  Service         │  │  Processor       │               │
│  └──────────────────┘  └──────────────────┘               │
│           │                      │                          │
│           │                      │                          │
│  ┌──────────────────────────────────────────┐             │
│  │         Data Mapper & Deduplication      │             │
│  │         Service                          │             │
│  └──────────────────────────────────────────┘             │
│                      │                                     │
└──────────────────────┼─────────────────────────────────────┘
                       │
                       ▼
            ┌──────────────────┐
            │    Database      │
            │   (PostgreSQL)   │
            └──────────────────┘
```



### 2.2 Technology Stack

**Frontend:**

- Angular 18 (Standalone Components)
- RxJS for reactive programming
- File handling libraries (ngx-file-drop, xlsx.js)
- WebSocket for real-time progress updates

**Backend:**

- Spring Boot 3.x
- Apache POI (for Excel parsing)
- OpenCSV (for CSV parsing)
- Spring Async (for background processing)
- WebSocket (for progress notifications)
- Redis (optional - for job queue)

---



## 3. Frontend Implementation



### 3.1 Component Structure

```
fe/src/app/features/patient-import/
├── components/
│   ├── patient-import-page/
│   │   ├── patient-import-page.component.ts
│   │   ├── patient-import-page.component.html
│   │   └── patient-import-page.component.css
│   ├── upload-zone/
│   │   ├── upload-zone.component.ts          # Drag & Drop area
│   │   ├── upload-zone.component.html
│   │   └── upload-zone.component.css
│   ├── import-config-panel/
│   │   ├── import-config-panel.component.ts  # Source system selector
│   │   ├── import-config-panel.component.html
│   │   └── import-config-panel.component.css
│   ├── data-preview/
│   │   ├── data-preview.component.ts         # Preview table
│   │   ├── data-preview.component.html
│   │   └── data-preview.component.css
│   ├── import-job-list/
│   │   ├── import-job-list.component.ts      # Active jobs queue
│   │   ├── import-job-list.component.html
│   │   └── import-job-list.component.css
│   └── column-mapping-dialog/
│       ├── column-mapping-dialog.component.ts # Manual column mapping
│       ├── column-mapping-dialog.component.html
│       └── column-mapping-dialog.component.css
├── services/
│   ├── file-upload.service.ts                # Handle file uploads
│   ├── import-job.service.ts                 # Import job management
│   └── websocket.service.ts                  # Real-time updates
├── models/
│   ├── import-job.model.ts
│   ├── import-config.model.ts
│   ├── file-validation-result.model.ts
│   └── column-mapping.model.ts
└── patient-import.routes.ts
```



### 3.2 Key Models (Frontend)

```typescript
// import-job.model.ts
export interface ImportJob {
  id: string;
  fileName: string;
  fileSize: number;
  sourceSystemId: number;
  sourceSystemName: string;
  status: ImportJobStatus;
  totalRows: number;
  processedRows: number;
  successfulRows: number;
  failedRows: number;
  duplicateRows: number;
  progress: number;
  startedAt: Date;
  completedAt?: Date;
  errorMessage?: string;
}

export enum ImportJobStatus {
  PENDING = 'PENDING',
  VALIDATING = 'VALIDATING',
  PROCESSING = 'PROCESSING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED'
}

// import-config.model.ts
export interface ImportConfig {
  sourceSystemId: number;
  mappingTemplate: 'AUTO' | 'MANUAL';
  skipDuplicates: boolean;
  duplicateThreshold: number; // 0-100
  validateBeforeImport: boolean;
  columnMappings?: ColumnMapping[];
}

export interface ColumnMapping {
  sourceColumn: string;
  targetField: string;
  transformation?: string; // e.g., "toUpperCase", "trim"
}

// file-validation-result.model.ts
export interface FileValidationResult {
  isValid: boolean;
  fileName: string;
  totalRows: number;
  validRows: number;
  invalidRows: number;
  columns: string[];
  detectedMappings: ColumnMapping[];
  errors: ValidationError[];
  warnings: ValidationWarning[];
  preview: PreviewRow[];
}

export interface PreviewRow {
  rowNumber: number;
  data: Record<string, any>;
  validationStatus: 'VALID' | 'WARNING' | 'ERROR';
  validationMessages: string[];
  matchScore?: number;
}

export interface ValidationError {
  rowNumber: number;
  column: string;
  message: string;
  severity: 'ERROR' | 'WARNING';
}
```



### 3.3 Upload Flow (Frontend)



#### Step 1: File Upload Component

```
┌─────────────────────────────────────────┐
│         Upload Zone Component           │
│                                         │
│  1. User drops file or clicks browse   │
│  2. Validate file type (.csv, .xlsx)   │
│  3. Validate file size (< 500MB)       │
│  4. Show file info preview              │
│  5. Call uploadService.upload()         │
└─────────────────────────────────────────┘
```

**Key Features:**

- Drag & drop với visual feedback
- File type validation (CSV, XLSX only)
- File size validation (max 500MB)
- Progress bar cho upload
- Cancel upload capability



#### Step 2: File Parsing & Preview

```
┌─────────────────────────────────────────┐
│      Backend Returns Validation         │
│                                         │
│  1. Parse first 50-100 rows             │
│  2. Detect column headers               │
│  3. Auto-map to target fields           │
│  4. Run validation rules                │
│  5. Calculate match scores              │
│  6. Return preview data                 │
└─────────────────────────────────────────┘
```

**Preview Table Features:**

- Display first 50 rows
- Color-coded validation status:
  - Green: Valid & High match (>95%)
  - Yellow: Valid but needs review (70-95%)
  - Red: Invalid or missing data
- Inline error messages
- Column mapping review
- Statistics summary



#### Step 3: Configuration & Start Import

```
┌─────────────────────────────────────────┐
│      Import Configuration Panel         │
│                                         │
│  1. Select source system                │
│  2. Choose mapping template             │
│  3. Set duplicate handling rules        │
│  4. Review column mappings              │
│  5. Click "Start Import"                │
└─────────────────────────────────────────┘
```



### 3.4 Real-time Progress Updates

```typescript
// WebSocket connection for progress updates
export class WebSocketService {
  private socket: WebSocket;
  
  connect(jobId: string): Observable<ImportProgressUpdate> {
    return new Observable(observer => {
      this.socket = new WebSocket(`ws://localhost:8080/ws/import/${jobId}`);
      
      this.socket.onmessage = (event) => {
        const update: ImportProgressUpdate = JSON.parse(event.data);
        observer.next(update);
      };
      
      this.socket.onerror = (error) => observer.error(error);
      this.socket.onclose = () => observer.complete();
    });
  }
}

interface ImportProgressUpdate {
  jobId: string;
  status: ImportJobStatus;
  progress: number;
  processedRows: number;
  totalRows: number;
  currentBatch: number;
  estimatedTimeRemaining: number; // seconds
}
```

---



## 4. Backend Implementation



### 4.1 Backend Structure

```
be/mpi/demo/src/main/java/com/mpi/demo/
├── controller/
│   └── ImportController.java
├── service/
│   ├── ImportService.java
│   ├── FileParserService.java
│   ├── ValidationService.java
│   ├── DataMapperService.java
│   ├── DeduplicationService.java
│   └── AsyncImportProcessor.java
├── dto/
│   ├── request/
│   │   ├── ImportConfigRequest.java
│   │   └── ColumnMappingRequest.java
│   └── response/
│       ├── FileValidationResponse.java
│       ├── ImportJobResponse.java
│       └── ImportProgressResponse.java
├── entity/
│   ├── ImportJob.java
│   ├── ImportJobDetail.java
│   └── ColumnMapping.java
├── repository/
│   ├── ImportJobRepository.java
│   └── ImportJobDetailRepository.java
└── config/
    ├── AsyncConfig.java
    └── WebSocketConfig.java
```



### 4.2 Database Schema cho Import Feature



#### 4.2.1 Cần thêm các bảng mới vào database

```sql
-- Bảng lưu thông tin import job
CREATE TABLE `import_job` (
    `id` bigint PRIMARY KEY AUTO_INCREMENT,
    `file_name` varchar(255) NOT NULL,
    `file_size` bigint NOT NULL COMMENT 'Kích thước file (bytes)',
    `file_type` ENUM('CSV', 'XLSX') NOT NULL,
    `source_system_id` bigint NOT NULL,
    `status` ENUM(
        'PENDING',
        'VALIDATING',
        'PROCESSING',
        'COMPLETED',
        'FAILED',
        'CANCELLED'
    ) NOT NULL DEFAULT 'PENDING',
    `total_rows` int DEFAULT 0,
    `processed_rows` int DEFAULT 0,
    `successful_rows` int DEFAULT 0,
    `failed_rows` int DEFAULT 0,
    `duplicate_rows` int DEFAULT 0,
    `warning_rows` int DEFAULT 0,
    `started_at` datetime,
    `completed_at` datetime,
    `error_message` text,
    `configuration` json COMMENT 'Cấu hình import: mapping, rules, etc.',
    `created_by` bigint NOT NULL,
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`source_system_id`) REFERENCES `source_system` (`id`),
    FOREIGN KEY (`created_by`) REFERENCES `users` (`id`)
);

-- Bảng lưu chi tiết từng row import
CREATE TABLE `import_job_detail` (
    `id` bigint PRIMARY KEY AUTO_INCREMENT,
    `import_job_id` bigint NOT NULL,
    `row_number` int NOT NULL,
    `row_data` json NOT NULL COMMENT 'Dữ liệu gốc của row',
    `status` ENUM(
        'SUCCESS',
        'FAILED',
        'DUPLICATE',
        'WARNING'
    ) NOT NULL,
    `match_score` decimal(5, 2) COMMENT 'Điểm match nếu tìm thấy duplicate',
    `patient_id` bigint COMMENT 'ID của patient đã tạo (nếu SUCCESS)',
    `matched_master_id` bigint COMMENT 'ID của patient_master đã match (nếu DUPLICATE)',
    `error_message` text,
    `warning_message` text,
    `processed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`import_job_id`) REFERENCES `import_job` (`id`) ON DELETE CASCADE,
    FOREIGN KEY (`patient_id`) REFERENCES `patient` (`id`) ON DELETE SET NULL,
    FOREIGN KEY (`matched_master_id`) REFERENCES `patient_master` (`id`) ON DELETE SET NULL
);

-- Index để query nhanh
CREATE INDEX `idx_import_job_status` ON `import_job` (`status`, `created_at`);
CREATE INDEX `idx_import_job_source` ON `import_job` (`source_system_id`, `created_at`);
CREATE INDEX `idx_import_detail_job` ON `import_job_detail` (`import_job_id`, `status`);
```



#### 4.2.2 Java Entities

```java
// ImportJob.java
@Entity
@Table(name = "import_job")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "file_name", nullable = false)
    private String fileName;
    
    @Column(name = "file_size", nullable = false)
    private Long fileSize;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false)
    private FileType fileType;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_system_id", nullable = false)
    private SourceSystem sourceSystem;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportJobStatus status;
    
    @Column(name = "total_rows")
    private Integer totalRows;
    
    @Column(name = "processed_rows")
    private Integer processedRows;
    
    @Column(name = "successful_rows")
    private Integer successfulRows;
    
    @Column(name = "failed_rows")
    private Integer failedRows;
    
    @Column(name = "duplicate_rows")
    private Integer duplicateRows;
    
    @Column(name = "warning_rows")
    private Integer warningRows;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(columnDefinition = "JSON")
    private String configuration;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "importJob", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ImportJobDetail> details = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

// ImportJobDetail.java
@Entity
@Table(name = "import_job_detail")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportJobDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_job_id", nullable = false)
    private ImportJob importJob;
    
    @Column(name = "row_number", nullable = false)
    private Integer rowNumber;
    
    @Column(name = "row_data", columnDefinition = "JSON", nullable = false)
    private String rowData;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportRowStatus status;
    
    @Column(name = "match_score", precision = 5, scale = 2)
    private BigDecimal matchScore;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private Patient patient;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_master_id")
    private PatientMaster matchedMaster;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "warning_message", columnDefinition = "TEXT")
    private String warningMessage;
    
    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;
    
    @PrePersist
    protected void onCreate() {
        if (processedAt == null) {
            processedAt = LocalDateTime.now();
        }
    }
}

// Enums
public enum FileType {
    CSV, XLSX
}

public enum ImportJobStatus {
    PENDING,      // Đang chờ xử lý
    VALIDATING,   // Đang validate dữ liệu
    PROCESSING,   // Đang xử lý import
    COMPLETED,    // Hoàn thành
    FAILED,       // Thất bại
    CANCELLED     // Đã hủy
}

public enum ImportRowStatus {
    SUCCESS,      // Import thành công
    FAILED,       // Import thất bại
    DUPLICATE,    // Phát hiện trùng lặp
    WARNING       // Import thành công nhưng có cảnh báo
}
```



### 4.3 Repositories

```java
// PatientRepository.java
public interface PatientRepository extends JpaRepository<Patient, Long> {
    
    // Tìm theo source system và local patient code (unique constraint)
    Optional<Patient> findBySourceSystemIdAndLocalPatientCode(
        Long sourceSystemId,
        String localPatientCode
    );
    
    // Tìm theo match status
    List<Patient> findByMatchStatus(MatchStatus matchStatus);
}

// PatientMasterRepository.java
public interface PatientMasterRepository extends JpaRepository<PatientMaster, Long> {
    
    // Blocking Strategy Queries
    
    // Block 1: DOB + First character of name
    @Query("SELECT pm FROM PatientMaster pm WHERE pm.dateOfBirth = ?1 AND UPPER(SUBSTRING(pm.fullName, 1, 1)) = ?2 AND pm.status = 'ACTIVE'")
    List<PatientMaster> findByDateOfBirthAndFullNameStartingWith(LocalDate dob, String firstChar);
    
    // Block 2: Exact National ID (highest priority)
    Optional<PatientMaster> findByNationalIdAndStatus(String nationalId, PatientStatus status);
    
    default PatientMaster findByNationalId(String nationalId) {
        return findByNationalIdAndStatus(nationalId, PatientStatus.ACTIVE).orElse(null);
    }
    
    // Block 3: Exact Health Insurance Number
    Optional<PatientMaster> findByHealthInsuranceNoAndStatus(String healthInsuranceNo, PatientStatus status);
    
    default PatientMaster findByHealthInsuranceNo(String healthInsuranceNo) {
        return findByHealthInsuranceNoAndStatus(healthInsuranceNo, PatientStatus.ACTIVE).orElse(null);
    }
    
    // Block 4: Phone Number
    List<PatientMaster> findByPhoneNumberAndStatus(String phoneNumber, PatientStatus status);
    
    default List<PatientMaster> findByPhoneNumber(String phoneNumber) {
        return findByPhoneNumberAndStatus(phoneNumber, PatientStatus.ACTIVE);
    }
    
    // Tìm theo enterprise ID
    Optional<PatientMaster> findByEnterpriseId(String enterpriseId);
}

// ImportJobRepository.java
public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {
    
    // Tìm jobs theo status
    List<ImportJob> findByStatusOrderByCreatedAtDesc(ImportJobStatus status);
    
    // Tìm jobs theo source system
    List<ImportJob> findBySourceSystemIdOrderByCreatedAtDesc(Long sourceSystemId);
    
    // Tìm jobs của user
    List<ImportJob> findByCreatedByIdOrderByCreatedAtDesc(Long userId);
    
    // Tìm jobs đang chạy
    @Query("SELECT ij FROM ImportJob ij WHERE ij.status IN ('PENDING', 'VALIDATING', 'PROCESSING') ORDER BY ij.createdAt DESC")
    List<ImportJob> findActiveJobs();
}

// ImportJobDetailRepository.java
public interface ImportJobDetailRepository extends JpaRepository<ImportJobDetail, Long> {
    
    // Tìm theo import job ID và status
    List<ImportJobDetail> findByImportJobIdAndStatus(Long importJobId, ImportRowStatus status);
    
    // Tìm tất cả details của một job
    List<ImportJobDetail> findByImportJobIdOrderByRowNumber(Long importJobId);
    
    // Count theo status
    @Query("SELECT COUNT(d) FROM ImportJobDetail d WHERE d.importJob.id = ?1 AND d.status = ?2")
    long countByImportJobIdAndStatus(Long importJobId, ImportRowStatus status);
}

// MatchCandidateRepository.java
public interface MatchCandidateRepository extends JpaRepository<MatchCandidate, Long> {
    
    // Tìm candidates theo patient
    List<MatchCandidate> findByPatientId(Long patientId);
    
    // Tìm pending candidates
    List<MatchCandidate> findByDecisionOrderByCreatedAtDesc(MatchDecision decision);
    
    // Tìm candidates có điểm cao
    @Query("SELECT mc FROM MatchCandidate mc WHERE mc.matchScore >= ?1 AND mc.decision = 'PENDING' ORDER BY mc.matchScore DESC")
    List<MatchCandidate> findHighScorePendingCandidates(BigDecimal minScore);
}

// SourceSystemRepository.java (Already exists, but add if needed)
public interface SourceSystemRepository extends JpaRepository<SourceSystem, Long> {
    
    Optional<SourceSystem> findByCode(String code);
    
    List<SourceSystem> findByIsActiveTrue();
    
    List<SourceSystem> findByFacilityId(Long facilityId);
}
```

**PatientStatus Enum (nếu cần):**

```java
public enum PatientStatus {
    ACTIVE,
    INACTIVE,
    MERGED
}
```



### 4.4 Import Controller API Endpoints

```java
@RestController
@RequestMapping("/api/import")
public class ImportController {
    
    // 1. Upload & Validate File
    @PostMapping("/upload")
    public ResponseEntity<FileValidationResponse> uploadFile(
        @RequestParam("file") MultipartFile file,
        @RequestParam("sourceSystemId") Long sourceSystemId
    ) {
        // Parse file, validate structure, return preview
    }
    
    // 2. Start Import Job
    @PostMapping("/start")
    public ResponseEntity<ImportJobResponse> startImport(
        @RequestBody ImportConfigRequest config
    ) {
        // Create job, start async processing
    }
    
    // 3. Get Job Status
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<ImportJobResponse> getJobStatus(
        @PathVariable String jobId
    ) {
        // Return current job status
    }
    
    // 4. List All Jobs
    @GetMapping("/jobs")
    public ResponseEntity<PageResponse<ImportJobResponse>> listJobs(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        // Return paginated job list
    }
    
    // 5. Cancel Job
    @PostMapping("/jobs/{jobId}/cancel")
    public ResponseEntity<Void> cancelJob(@PathVariable String jobId) {
        // Cancel running job
    }
    
    // 6. Download Error Report
    @GetMapping("/jobs/{jobId}/errors")
    public ResponseEntity<Resource> downloadErrorReport(
        @PathVariable String jobId
    ) {
        // Generate CSV with failed rows
    }
    
    // 7. Get Column Mapping Suggestions
    @PostMapping("/suggest-mapping")
    public ResponseEntity<List<ColumnMappingResponse>> suggestMapping(
        @RequestBody List<String> columns
    ) {
        // Auto-detect column mappings
    }
}
```



### 4.5 Deduplication Service

```java
@Service
public class DeduplicationService {
    
    @Autowired
    private PatientRepository patientRepository;
    
    @Autowired
    private PatientMasterRepository patientMasterRepository;
    
    @Autowired
    private MatchCandidateRepository matchCandidateRepository;
    
    /**
     * Tìm kiếm duplicate cho một patient mới
     * Returns: DuplicationResult với match score cao nhất
     * 
     * Sử dụng Blocking Strategy để giảm số lượng records cần so sánh
     */
    public DuplicationResult findDuplicates(Patient newPatient) {
        // BLOCKING STRATEGY: Chỉ tìm trong những records có potential match
        // Thay vì findByFullNameContaining (quét toàn bộ), 
        // sử dụng multiple blocking keys để giảm search space
        
        List<PatientMaster> candidates = new ArrayList<>();
        
        // Block 1: Exact date of birth + first character of name
        if (newPatient.getDateOfBirth() != null && newPatient.getFullName() != null) {
            String firstChar = newPatient.getFullName().substring(0, 1).toUpperCase();
            candidates.addAll(
                patientMasterRepository.findByDateOfBirthAndFullNameStartingWith(
                    newPatient.getDateOfBirth(),
                    firstChar
                )
            );
        }
        
        // Block 2: Exact national ID (highest priority)
        if (newPatient.getNationalId() != null && !newPatient.getNationalId().isEmpty()) {
            PatientMaster byNationalId = patientMasterRepository
                .findByNationalId(newPatient.getNationalId());
            if (byNationalId != null && !candidates.contains(byNationalId)) {
                candidates.add(byNationalId);
            }
        }
        
        // Block 3: Exact health insurance number
        if (newPatient.getHealthInsuranceNo() != null && !newPatient.getHealthInsuranceNo().isEmpty()) {
            PatientMaster byHealthInsurance = patientMasterRepository
                .findByHealthInsuranceNo(newPatient.getHealthInsuranceNo());
            if (byHealthInsurance != null && !candidates.contains(byHealthInsurance)) {
                candidates.add(byHealthInsurance);
            }
        }
        
        // Block 4: Phone number (last resort)
        if (candidates.isEmpty() && newPatient.getPhoneNumber() != null) {
            candidates.addAll(
                patientMasterRepository.findByPhoneNumber(newPatient.getPhoneNumber())
            );
        }
        
        // 3. Calculate match scores for candidates only
        BigDecimal highestScore = BigDecimal.ZERO;
        PatientMaster bestMatch = null;
        Map<String, BigDecimal> bestScoreBreakdown = new HashMap<>();
        
        for (PatientMaster master : candidates) {
            Map<String, BigDecimal> scoreBreakdown = new HashMap<>();
            BigDecimal score = calculateMatchScore(newPatient, master, scoreBreakdown);
            
            if (score.compareTo(highestScore) > 0) {
                highestScore = score;
                bestMatch = master;
                bestScoreBreakdown = scoreBreakdown;
            }
        }
        
        // If high score found, it's a duplicate
        boolean isDuplicate = highestScore.compareTo(BigDecimal.valueOf(70)) >= 0;
        
        return DuplicationResult.builder()
            .isDuplicate(isDuplicate)
            .matchScore(highestScore)
            .matchedMaster(bestMatch)
            .scoreBreakdown(bestScoreBreakdown)
            .build();
    }
    
    /**
     * Calculate match score giữa Patient và PatientMaster
     * Sử dụng weighted scoring theo từng field với veto rule và weight redistribution
     */
    private BigDecimal calculateMatchScore(
        Patient patient,
        PatientMaster master,
        Map<String, BigDecimal> scoreBreakdown
    ) {
        // VETO RULE: Nếu giới tính khác nhau → return 0 (không match)
        if (patient.getGender() != null && master.getGender() != null) {
            if (!patient.getGender().equals(master.getGender())) {
                scoreBreakdown.put("veto_gender_mismatch", BigDecimal.ZERO);
                return BigDecimal.ZERO;
            }
        }
        
        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal totalPossibleWeight = BigDecimal.valueOf(100);
        BigDecimal missingWeight = BigDecimal.ZERO;
        
        // Define weights
        BigDecimal nameWeight = BigDecimal.valueOf(30);
        BigDecimal dobWeight = BigDecimal.valueOf(25);
        BigDecimal nationalIdWeight = BigDecimal.valueOf(20);
        BigDecimal healthInsuranceWeight = BigDecimal.valueOf(15);
        BigDecimal phoneWeight = BigDecimal.valueOf(10);
        
        // 1. Full Name matching (weight: 30)
        BigDecimal nameScore = calculateNameSimilarity(
            patient.getFullName(),
            master.getFullName()
        ).multiply(nameWeight);
        scoreBreakdown.put("fullName", nameScore);
        totalScore = totalScore.add(nameScore);
        
        // 2. Date of Birth matching (weight: 25)
        if (patient.getDateOfBirth() != null && master.getDateOfBirth() != null) {
            if (patient.getDateOfBirth().equals(master.getDateOfBirth())) {
                scoreBreakdown.put("dateOfBirth", dobWeight);
                totalScore = totalScore.add(dobWeight);
            } else {
                scoreBreakdown.put("dateOfBirth", BigDecimal.ZERO);
            }
        } else {
            // Missing field - track for redistribution
            missingWeight = missingWeight.add(dobWeight);
            scoreBreakdown.put("dateOfBirth", null);
        }
        
        // 3. National ID matching (weight: 20)
        if (patient.getNationalId() != null && master.getNationalId() != null &&
            !patient.getNationalId().isEmpty() && !master.getNationalId().isEmpty()) {
            if (patient.getNationalId().equals(master.getNationalId())) {
                scoreBreakdown.put("nationalId", nationalIdWeight);
                totalScore = totalScore.add(nationalIdWeight);
            } else {
                scoreBreakdown.put("nationalId", BigDecimal.ZERO);
            }
        } else {
            missingWeight = missingWeight.add(nationalIdWeight);
            scoreBreakdown.put("nationalId", null);
        }
        
        // 4. Health Insurance Number matching (weight: 15)
        if (patient.getHealthInsuranceNo() != null && master.getHealthInsuranceNo() != null &&
            !patient.getHealthInsuranceNo().isEmpty() && !master.getHealthInsuranceNo().isEmpty()) {
            if (patient.getHealthInsuranceNo().equals(master.getHealthInsuranceNo())) {
                scoreBreakdown.put("healthInsuranceNo", healthInsuranceWeight);
                totalScore = totalScore.add(healthInsuranceWeight);
            } else {
                scoreBreakdown.put("healthInsuranceNo", BigDecimal.ZERO);
            }
        } else {
            missingWeight = missingWeight.add(healthInsuranceWeight);
            scoreBreakdown.put("healthInsuranceNo", null);
        }
        
        // 5. Phone Number matching (weight: 10)
        if (patient.getPhoneNumber() != null && master.getPhoneNumber() != null &&
            !patient.getPhoneNumber().isEmpty() && !master.getPhoneNumber().isEmpty()) {
            BigDecimal phoneScore = calculatePhoneSimilarity(
                patient.getPhoneNumber(),
                master.getPhoneNumber()
            ).multiply(phoneWeight);
            scoreBreakdown.put("phoneNumber", phoneScore);
            totalScore = totalScore.add(phoneScore);
        } else {
            missingWeight = missingWeight.add(phoneWeight);
            scoreBreakdown.put("phoneNumber", null);
        }
        
        // WEIGHT REDISTRIBUTION: Nếu có field thiếu, scale lại score
        if (missingWeight.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal availableWeight = totalPossibleWeight.subtract(missingWeight);
            if (availableWeight.compareTo(BigDecimal.ZERO) > 0) {
                // Scale score to 100
                totalScore = totalScore.multiply(totalPossibleWeight)
                    .divide(availableWeight, 2, RoundingMode.HALF_UP);
            }
            scoreBreakdown.put("weight_redistribution", 
                BigDecimal.valueOf(100).subtract(availableWeight));
        }
        
        return totalScore.min(BigDecimal.valueOf(100)); // Cap at 100
    }
    
    /**
     * Tính độ tương đồng tên sử dụng Levenshtein Distance
     */
    private BigDecimal calculateNameSimilarity(String name1, String name2) {
        if (name1 == null || name2 == null) {
            return BigDecimal.ZERO;
        }
        
        // Normalize names
        String n1 = normalizeVietnameseName(name1);
        String n2 = normalizeVietnameseName(name2);
        
        if (n1.equals(n2)) {
            return BigDecimal.ONE;
        }
        
        // Calculate Levenshtein distance
        int distance = levenshteinDistance(n1, n2);
        int maxLength = Math.max(n1.length(), n2.length());
        
        if (maxLength == 0) {
            return BigDecimal.ONE;
        }
        
        double similarity = 1.0 - ((double) distance / maxLength);
        return BigDecimal.valueOf(similarity);
    }
    
    /**
     * Normalize Vietnamese name: loại bỏ dấu, chuyển lowercase, trim
     */
    private String normalizeVietnameseName(String name) {
        if (name == null) return "";
        
        String normalized = name.toLowerCase().trim();
        
        // Remove Vietnamese diacritics
        normalized = normalized.replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a");
        normalized = normalized.replaceAll("[èéẹẻẽêềếệểễ]", "e");
        normalized = normalized.replaceAll("[ìíịỉĩ]", "i");
        normalized = normalized.replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o");
        normalized = normalized.replaceAll("[ùúụủũưừứựửữ]", "u");
        normalized = normalized.replaceAll("[ỳýỵỷỹ]", "y");
        normalized = normalized.replaceAll("[đ]", "d");
        
        // Remove extra spaces
        normalized = normalized.replaceAll("\\s+", " ");
        
        return normalized;
    }
    
    /**
     * Calculate Levenshtein Distance
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    /**
     * Tính độ tương đồng số điện thoại
     */
    private BigDecimal calculatePhoneSimilarity(String phone1, String phone2) {
        if (phone1 == null || phone2 == null) {
            return BigDecimal.ZERO;
        }
        
        // Normalize: remove spaces, dashes, parentheses
        String p1 = phone1.replaceAll("[\\s\\-()]", "");
        String p2 = phone2.replaceAll("[\\s\\-()]", "");
        
        // Exact match
        if (p1.equals(p2)) {
            return BigDecimal.ONE;
        }
        
        // Check last 7 digits (local number)
        if (p1.length() >= 7 && p2.length() >= 7) {
            String last7_1 = p1.substring(p1.length() - 7);
            String last7_2 = p2.substring(p2.length() - 7);
            
            if (last7_1.equals(last7_2)) {
                return BigDecimal.valueOf(0.8); // 80% match
            }
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * Save match candidate cho manual review
     */
    public void saveMatchCandidate(
        Patient patient,
        PatientMaster master,
        BigDecimal score,
        Map<String, BigDecimal> scoreBreakdown
    ) {
        MatchCandidate candidate = new MatchCandidate();
        candidate.setPatient(patient);
        candidate.setCandidateMaster(master);
        candidate.setMatchScore(score);
        candidate.setWeightVersion("v1");
        
        // Convert score breakdown to JSON
        try {
            ObjectMapper mapper = new ObjectMapper();
            String breakdownJson = mapper.writeValueAsString(scoreBreakdown);
            candidate.setScoreBreakdown(breakdownJson);
        } catch (Exception e) {
            candidate.setScoreBreakdown("{}");
        }
        
        // Auto approve if score > 98
        if (score.compareTo(BigDecimal.valueOf(98)) > 0) {
            candidate.setDecision(MatchDecision.AUTO_APPROVED);
        } else {
            candidate.setDecision(MatchDecision.PENDING);
        }
        
        matchCandidateRepository.save(candidate);
    }
}

/**
 * Result class for duplication check
 */
@Data
@Builder
public class DuplicationResult {
    private boolean isDuplicate;
    private BigDecimal matchScore;
    private PatientMaster matchedMaster;
    private Map<String, BigDecimal> scoreBreakdown;
}
```



```java
@Service
public class FileParserService {
    
    /**
     * Parse CSV file and return structured data
     */
    public ParsedFileData parseCSV(MultipartFile file) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();
        
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] headers = reader.readNext();
            String[] line;
            int rowNumber = 1;
            
            while ((line = reader.readNext()) != null && rowNumber <= 100) {
                Map<String, String> row = new HashMap<>();
                for (int i = 0; i < headers.length && i < line.length; i++) {
                    row.put(headers[i], line[i]);
                }
                row.put("_rowNumber", String.valueOf(rowNumber++));
                rows.add(row);
            }
            
            return ParsedFileData.builder()
                .headers(List.of(headers))
                .rows(rows)
                .totalRows(countTotalRows(file))
                .build();
        }
    }
    
    /**
     * Parse XLSX file and return structured data
     */
    public ParsedFileData parseExcel(MultipartFile file) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();
        
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // Read headers from first row
            Row headerRow = sheet.getRow(0);
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(cell.getStringCellValue());
            }
            
            // Read data rows (max 100 for preview)
            int rowNumber = 1;
            for (int i = 1; i <= Math.min(100, sheet.getLastRowNum()); i++) {
                Row dataRow = sheet.getRow(i);
                if (dataRow == null) continue;
                
                Map<String, String> row = new HashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = dataRow.getCell(j);
                    row.put(headers.get(j), getCellValueAsString(cell));
                }
                row.put("_rowNumber", String.valueOf(rowNumber++));
                rows.add(row);
            }
            
            return ParsedFileData.builder()
                .headers(headers)
                .rows(rows)
                .totalRows(sheet.getLastRowNum())
                .build();
        }
    }
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: 
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA: return cell.getCellFormula();
            default: return "";
        }
    }
}
```



### 4.5 Validation Service

```java
@Service
public class ValidationService {
    
    /**
     * Validate a single row of patient data
     */
    public RowValidationResult validateRow(
        Map<String, String> rowData,
        Map<String, String> columnMapping
    ) {
        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();
        
        // 1. Required fields validation
        if (isEmpty(rowData.get(columnMapping.get("fullName")))) {
            errors.add(new ValidationError("fullName", "Họ tên không được để trống"));
        }
        
        if (isEmpty(rowData.get(columnMapping.get("dateOfBirth")))) {
            errors.add(new ValidationError("dateOfBirth", "Ngày sinh không được để trống"));
        }
        
        if (isEmpty(rowData.get(columnMapping.get("gender")))) {
            errors.add(new ValidationError("gender", "Giới tính không được để trống"));
        }
        
        // 2. Format validation
        String dob = rowData.get(columnMapping.get("dateOfBirth"));
        if (dob != null && !isValidDate(dob)) {
            errors.add(new ValidationError("dateOfBirth", 
                "Định dạng ngày sinh không hợp lệ. Yêu cầu: YYYY-MM-DD"));
        }
        
        String gender = rowData.get(columnMapping.get("gender"));
        if (gender != null && !isValidGender(gender)) {
            errors.add(new ValidationError("gender", 
                "Giới tính không hợp lệ. Chỉ chấp nhận: M, F, O"));
        }
        
        String phoneNumber = rowData.get(columnMapping.get("phoneNumber"));
        if (phoneNumber != null && !isValidPhoneNumber(phoneNumber)) {
            warnings.add(new ValidationWarning("phoneNumber", 
                "Số điện thoại không đúng định dạng"));
        }
        
        // 3. Business rules validation
        if (dob != null && isFutureDate(dob)) {
            errors.add(new ValidationError("dateOfBirth", 
                "Ngày sinh không thể là ngày trong tương lai"));
        }
        
        if (dob != null && calculateAge(dob) > 150) {
            warnings.add(new ValidationWarning("dateOfBirth", 
                "Tuổi bệnh nhân có vẻ không hợp lý (>150 tuổi)"));
        }
        
        return RowValidationResult.builder()
            .isValid(errors.isEmpty())
            .errors(errors)
            .warnings(warnings)
            .build();
    }
    
    private boolean isValidDate(String date) {
        try {
            LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isValidGender(String gender) {
        return List.of("M", "F", "O", "MALE", "FEMALE", "OTHER").contains(gender.toUpperCase());
    }
    
    private boolean isValidPhoneNumber(String phone) {
        return phone.matches("^[0-9+\\-\\s()]{10,15}$");
    }
}
```



### 4.6 Async Import Processor

```java
@Service
public class AsyncImportProcessor {
    
    private static final int BATCH_SIZE = 100;
    
    @Autowired
    private PatientRepository patientRepository;
    
    @Autowired
    private PatientMasterRepository patientMasterRepository;
    
    @Autowired
    private ImportJobRepository importJobRepository;
    
    @Autowired
    private ImportJobDetailRepository importJobDetailRepository;
    
    @Autowired
    private ValidationService validationService;
    
    @Autowired
    private DeduplicationService deduplicationService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate; // WebSocket
    
    /**
     * Process import job asynchronously
     */
    @Async("importTaskExecutor")
    public CompletableFuture<Void> processImportJob(
        Long jobId,
        List<Map<String, String>> allRows,
        ImportConfig config
    ) {
        ImportJob job = importJobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Job not found"));
        
        try {
            job.setStatus(ImportJobStatus.PROCESSING);
            job.setStartedAt(LocalDateTime.now());
            job.setTotalRows(allRows.size());
            importJobRepository.save(job);
            
            int processedCount = 0;
            int successCount = 0;
            int failedCount = 0;
            int duplicateCount = 0;
            int warningCount = 0;
            
            // Process in batches
            for (int i = 0; i < allRows.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, allRows.size());
                List<Map<String, String>> batch = allRows.subList(i, end);
                
                List<ImportJobDetail> batchDetails = new ArrayList<>();
                
                for (Map<String, String> rowData : batch) {
                    ImportJobDetail detail = new ImportJobDetail();
                    detail.setImportJob(job);
                    detail.setRowNumber(processedCount + 1);
                    detail.setRowData(convertToJson(rowData));
                    
                    try {
                        // 1. Validate row
                        RowValidationResult validation = validationService.validateRow(
                            rowData, config.getColumnMappings()
                        );
                        
                        if (!validation.isValid()) {
                            detail.setStatus(ImportRowStatus.FAILED);
                            detail.setErrorMessage(formatErrors(validation.getErrors()));
                            failedCount++;
                            batchDetails.add(detail);
                            continue;
                        }
                        
                        // 2. Map data to Patient entity
                        Patient patient = mapToPatient(rowData, config, job.getSourceSystem());
                        
                        // 3. Check for duplicates in existing patients
                        DuplicationResult dupResult = deduplicationService.findDuplicates(patient);
                        
                        if (dupResult.isDuplicate() && 
                            dupResult.getMatchScore().compareTo(
                                BigDecimal.valueOf(config.getDuplicateThreshold())
                            ) > 0) {
                            
                            // BẮT BUỘC: Save match candidate cho manual review
                            deduplicationService.saveMatchCandidate(
                                patient,
                                dupResult.getMatchedMaster(),
                                dupResult.getMatchScore(),
                                dupResult.getScoreBreakdown()
                            );
                            
                            if (config.isSkipDuplicates()) {
                                detail.setStatus(ImportRowStatus.DUPLICATE);
                                detail.setMatchScore(dupResult.getMatchScore());
                                detail.setMatchedMaster(dupResult.getMatchedMaster());
                                duplicateCount++;
                                batchDetails.add(detail);
                                continue;
                            }
                        }
                        
                        // 4. Set match status
                        patient.setMatchStatus(MatchStatus.PENDING);
                        
                        // 5. Save patient
                        Patient savedPatient = patientRepository.save(patient);
                        
                        detail.setStatus(ImportRowStatus.SUCCESS);
                        detail.setPatient(savedPatient);
                        
                        if (!validation.getWarnings().isEmpty()) {
                            detail.setWarningMessage(formatWarnings(validation.getWarnings()));
                            warningCount++;
                        }
                        
                        successCount++;
                        batchDetails.add(detail);
                        
                    } catch (Exception e) {
                        detail.setStatus(ImportRowStatus.FAILED);
                        detail.setErrorMessage("System error: " + e.getMessage());
                        failedCount++;
                        batchDetails.add(detail);
                    }
                    
                    processedCount++;
                }
                
                // Save batch details
                importJobDetailRepository.saveAll(batchDetails);
                
                // Update job progress
                job.setProcessedRows(processedCount);
                job.setSuccessfulRows(successCount);
                job.setFailedRows(failedCount);
                job.setDuplicateRows(duplicateCount);
                job.setWarningRows(warningCount);
                importJobRepository.save(job);
                
                // Send progress update via WebSocket
                sendProgressUpdate(job, processedCount, allRows.size());
            }
            
            // Mark job as completed
            job.setStatus(ImportJobStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());
            importJobRepository.save(job);
            
            sendProgressUpdate(job, processedCount, allRows.size());
            
        } catch (Exception e) {
            job.setStatus(ImportJobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            importJobRepository.save(job);
            throw e;
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Map row data to Patient entity
     */
    private Patient mapToPatient(
        Map<String, String> rowData,
        ImportConfig config,
        SourceSystem sourceSystem
    ) {
        Map<String, String> mappings = config.getColumnMappings();
        
        Patient patient = new Patient();
        patient.setSourceSystem(sourceSystem);
        patient.setLocalPatientCode(rowData.get(mappings.get("localPatientCode")));
        patient.setFullName(rowData.get(mappings.get("fullName")));
        
        // Parse date of birth
        String dobStr = rowData.get(mappings.get("dateOfBirth"));
        if (dobStr != null && !dobStr.isEmpty()) {
            patient.setDateOfBirth(LocalDate.parse(dobStr));
        }
        
        // Parse gender
        String genderStr = rowData.get(mappings.get("gender"));
        if (genderStr != null && !genderStr.isEmpty()) {
            patient.setGender(parseGender(genderStr));
        }
        
        patient.setNationalId(rowData.get(mappings.get("nationalId")));
        patient.setHealthInsuranceNo(rowData.get(mappings.get("healthInsuranceNo")));
        patient.setPhoneNumber(rowData.get(mappings.get("phoneNumber")));
        patient.setAddress(rowData.get(mappings.get("address")));
        
        return patient;
    }
    
    private Gender parseGender(String genderStr) {
        String normalized = genderStr.toUpperCase().trim();
        switch (normalized) {
            case "M":
            case "MALE":
            case "NAM":
                return Gender.MALE;
            case "F":
            case "FEMALE":
            case "NỮ":
            case "NU":
                return Gender.FEMALE;
            default:
                return Gender.OTHER;
        }
    }
    
    private String convertToJson(Map<String, String> data) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }
    
    private String formatErrors(List<ValidationError> errors) {
        return errors.stream()
            .map(e -> e.getField() + ": " + e.getMessage())
            .collect(Collectors.joining("; "));
    }
    
    private String formatWarnings(List<ValidationWarning> warnings) {
        return warnings.stream()
            .map(w -> w.getField() + ": " + w.getMessage())
            .collect(Collectors.joining("; "));
    }
    
    private void sendProgressUpdate(ImportJob job, int processed, int total) {
        ImportProgressUpdate update = ImportProgressUpdate.builder()
            .jobId(job.getId().toString())
            .status(job.getStatus())
            .progress((int) ((processed * 100.0) / total))
            .processedRows(processed)
            .totalRows(total)
            .successfulRows(job.getSuccessfulRows())
            .failedRows(job.getFailedRows())
            .duplicateRows(job.getDuplicateRows())
            .warningRows(job.getWarningRows())
            .build();
        
        messagingTemplate.convertAndSend(
            "/topic/import/" + job.getId(),
            update
        );
    }
}
```



---



## 5. Flow Xử Lý Import



### 5.1 Complete Import Flow Diagram

```
User Action                 Frontend                    Backend                     Database
    │                           │                           │                           │
    │  1. Upload File           │                           │                           │
    ├──────────────────────────>│                           │                           │
    │                           │  2. POST /api/import/upload                           │
    │                           ├──────────────────────────>│                           │
    │                           │                           │  3. Parse File            │
    │                           │                           │    (first 100 rows)       │
    │                           │                           │                           │
    │                           │                           │  4. Detect Columns        │
    │                           │                           │                           │
    │                           │                           │  5. Validate Rows         │
    │                           │                           │                           │
    │                           │  6. Return Preview        │                           │
    │                           │<──────────────────────────┤                           │
    │  7. Show Preview          │                           │                           │
    │<──────────────────────────┤                           │                           │
    │                           │                           │                           │
    │  8. Review & Configure    │                           │                           │
    ├──────────────────────────>│                           │                           │
    │                           │                           │                           │
    │  9. Click "Start Import"  │                           │                           │
    ├──────────────────────────>│                           │                           │
    │                           │  10. POST /api/import/start                           │
    │                           ├──────────────────────────>│                           │
    │                           │                           │  11. Create Import Job    │
    │                           │                           ├──────────────────────────>│
    │                           │                           │                           │
    │                           │  12. Return Job ID        │                           │
    │                           │<──────────────────────────┤                           │
    │                           │                           │                           │
    │                           │  13. Connect WebSocket    │                           │
    │                           ├──────────────────────────>│                           │
    │                           │                           │                           │
    │                           │                           │  14. Start Async Process  │
    │                           │                           │  ┌─────────────────────┐  │
    │                           │                           │  │ For each batch:     │  │
    │                           │                           │  │ - Validate          │  │
    │                           │                           │  │ - Check duplicates  │  │
    │                           │                           │  │ - Save to DB        │  │
    │                           │                           │  └─────────────────────┘  │
    │                           │                           │                           │
    │                           │  15. Progress Updates     │                           │
    │                           │<─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─│                           │
    │  16. Update Progress UI   │                           │                           │
    │<─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─│                           │                           │
    │                           │                           │                           │
    │                           │                           │  17. Save Patients        │
    │                           │                           ├──────────────────────────>│
    │                           │                           │                           │
    │                           │  18. Completion Notice    │                           │
    │                           │<──────────────────────────┤                           │
    │  19. Show Summary         │                           │                           │
    │<──────────────────────────┤                           │                           │
```



### 5.2 Error Handling Flow

```
┌──────────────────────────────────────────────────┐
│              Row Processing Loop                 │
└──────────────────────────────────────────────────┘
                      │
                      ▼
          ┌───────────────────────┐
          │   Validate Row Data   │
          └───────────────────────┘
                      │
        ┌─────────────┴─────────────┐
        │                           │
        ▼                           ▼
   [Valid]                     [Invalid]
        │                           │
        ▼                           ▼
┌──────────────────┐      ┌──────────────────┐
│ Check Duplicates │      │ Save to Failed   │
└──────────────────┘      │ Rows Table       │
        │                 └──────────────────┘
        │                          │
┌───────┴────────┐                 │
│                │                 │
▼                ▼                 ▼
[No Dup]    [Duplicate]     Continue to
    │            │           Next Row
    │            │
    │    ┌───────┴──────┐
    │    │              │
    │    ▼              ▼
    │ [Auto-  [Manual
    │  Skip]   Review]
    │    │              │
    │    │      ┌───────┘
    │    │      │
    │    ▼      ▼
    │  Skip  Queue for
    │  Row   Manual Review
    │    │      │
    ▼    ▼      ▼
┌─────────────────────┐
│   Save Patient      │
│   Record to DB      │
└─────────────────────┘
        │
        ▼
  Continue to
   Next Row
```

---



## 6. Validation & Error Handling



### 6.1 Validation Levels

**Level 1: File Validation**

- File type (CSV, XLSX only)
- File size (< 500MB)
- File structure (has headers)
- File encoding (UTF-8, UTF-16)

**Level 2: Column Validation**

- Required columns present
- Column name mapping
- Data type compatibility

**Level 3: Row Validation**

- Required fields not empty
- Data format validation:
  - Date format (YYYY-MM-DD)
  - Phone number format
  - Email format
  - Gender values
- Business rules:
  - Age must be reasonable
  - DOB cannot be future date
  - Phone number length

**Level 4: Cross-row Validation**

- Duplicate detection within file
- Duplicate detection against database
- Data consistency checks



### 6.2 Error Categories & Handling

```java
public enum ErrorCategory {
    CRITICAL,    // Cannot proceed, must fix
    WARNING,     // Can proceed, but needs review
    INFO         // Informational only
}

// Error Handling Strategy
┌─────────────────────────────────────────────────────┐
│ CRITICAL Errors:                                    │
│ - Missing required fields                           │
│ - Invalid data types                                │
│ - Business rule violations                          │
│ → Action: Skip row, log error, continue            │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│ WARNING Errors:                                     │
│ - Potential duplicates (70-95% match)               │
│ - Format inconsistencies                            │
│ - Unusual values                                    │
│ → Action: Import with warning flag, queue review   │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│ INFO Messages:                                      │
│ - Data transformations applied                      │
│ - Auto-corrections made                             │
│ → Action: Log information, proceed                 │
└─────────────────────────────────────────────────────┘
```



### 6.3 Error Report Generation

```java
@Service
public class ErrorReportService {
    
    @Autowired
    private ImportJobRepository importJobRepository;
    
    @Autowired
    private ImportJobDetailRepository importJobDetailRepository;
    
    /**
     * Generate CSV error report for failed imports
     */
    public File generateErrorReport(Long jobId) {
        ImportJob job = importJobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        
        List<ImportJobDetail> failedRows = importJobDetailRepository
            .findByImportJobIdAndStatus(jobId, ImportRowStatus.FAILED);
        
        // Create CSV with original data + error messages
        File reportFile = new File("error_report_" + jobId + ".csv");
        
        try (CSVWriter writer = new CSVWriter(new FileWriter(reportFile))) {
            // Write header
            String[] header = {"Row Number", "Error Message", "Original Data"};
            writer.writeNext(header);
            
            // Write error rows
            for (ImportJobDetail detail : failedRows) {
                String[] row = {
                    detail.getRowNumber().toString(),
                    detail.getErrorMessage(),
                    detail.getRowData()
                };
                writer.writeNext(row);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate error report", e);
        }
        
        return reportFile;
    }
}
```

---



## 7. Performance & Scalability



### 7.1 Performance Optimizations

**1. Batch Processing**

```java
// Process in batches of 100 rows
private static final int BATCH_SIZE = 100;

for (int i = 0; i < allRows.size(); i += BATCH_SIZE) {
    List<Map<String, String>> batch = allRows.subList(i, 
        Math.min(i + BATCH_SIZE, allRows.size()));
    
    // Process batch
    List<Patient> patients = batch.stream()
        .map(this::mapAndValidate)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    
    // Bulk insert
    patientRepository.saveAll(patients);
    
    // Commit transaction after each batch
    entityManager.flush();
    entityManager.clear();
}
```

**2. Async Processing**

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "importTaskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("import-");
        executor.initialize();
        return executor;
    }
}
```

**3. Database Indexing**

```sql
-- Indexes for faster duplicate detection
CREATE INDEX idx_patient_name_dob ON patients(full_name, date_of_birth);
CREATE INDEX idx_patient_national_id ON patients(national_id);
CREATE INDEX idx_patient_phone ON patients(phone_number);

-- Index for import job queries
CREATE INDEX idx_import_job_status ON import_jobs(status, created_at);
```

**4. Caching**

```java
@Service
public class SourceSystemCache {
    
    private final Map<Long, SourceSystem> cache = new ConcurrentHashMap<>();
    
    @Cacheable(value = "sourceSystems", key = "#id")
    public SourceSystem getSourceSystem(Long id) {
        return sourceSystemRepository.findById(id).orElse(null);
    }
}
```



### 7.2 Scalability Considerations

**Database Indexes cho Performance**

```sql
-- Indexes cho duplicate detection (dựa trên db schema thực tế)
CREATE INDEX idx_patient_fullname ON patient(full_name);
CREATE INDEX idx_patient_dob_gender ON patient(date_of_birth, gender);
CREATE INDEX idx_patient_national_id ON patient(national_id);
CREATE INDEX idx_patient_health_insurance ON patient(health_insurance_no);
CREATE INDEX idx_patient_phone ON patient(phone_number);
CREATE INDEX idx_patient_source_match ON patient(source_system_id, match_status);

-- Indexes cho patient_master
CREATE INDEX idx_master_fullname ON patient_master(full_name);
CREATE INDEX idx_master_dob ON patient_master(date_of_birth);
CREATE INDEX idx_master_national_id ON patient_master(national_id);
CREATE INDEX idx_master_enterprise_id ON patient_master(enterprise_id);

-- Indexes cho import job queries (đã có trong schema)
-- CREATE INDEX idx_import_job_status ON import_job(status, created_at);
-- CREATE INDEX idx_import_job_source ON import_job(source_system_id, created_at);
-- CREATE INDEX idx_import_detail_job ON import_job_detail(import_job_id, status);

-- Composite index for fast duplicate lookup
CREATE INDEX idx_patient_match_lookup ON patient(
    source_system_id,
    full_name,
    date_of_birth
);
```

```
┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│   Upload     │      │   Upload     │      │   Upload     │
│   Service    │      │   Service    │      │   Service    │
│  Instance 1  │      │  Instance 2  │      │  Instance 3  │
└──────┬───────┘      └──────┬───────┘      └──────┬───────┘
       │                     │                     │
       │                     ▼                     │
       └─────────────> Redis Queue <───────────────┘
                            │
                            │
              ┌─────────────┴─────────────┐
              │                           │
              ▼                           ▼
    ┌──────────────────┐        ┌──────────────────┐
    │   Worker         │        │   Worker         │
    │   Instance 1     │        │   Instance 2     │
    └──────────────────┘        └──────────────────┘
              │                           │
              └─────────────┬─────────────┘
                            ▼
                      ┌──────────┐
                      │ Database │
                      └──────────┘
```

**Memory Management**

```java
// Stream large files instead of loading all into memory
public void processLargeFile(MultipartFile file) {
    try (InputStream is = file.getInputStream();
         BufferedReader reader = new BufferedReader(
             new InputStreamReader(is))) {
        
        String line;
        List<String[]> batch = new ArrayList<>();
        
        while ((line = reader.readLine()) != null) {
            String[] row = line.split(",");
            batch.add(row);
            
            if (batch.size() >= BATCH_SIZE) {
                processBatch(batch);
                batch.clear(); // Free memory
            }
        }
        
        // Process remaining
        if (!batch.isEmpty()) {
            processBatch(batch);
        }
    }
}
```

---



## 8. Security Considerations



### 8.1 File Upload Security

**1. File Type Validation**

```java
@Component
public class FileValidator {
    
    private static final List<String> ALLOWED_TYPES = List.of(
        "text/csv",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );
    
    public void validateFile(MultipartFile file) {
        // Check MIME type
        String contentType = file.getContentType();
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new InvalidFileTypeException("File type not allowed");
        }
        
        // Check file extension
        String filename = file.getOriginalFilename();
        if (!filename.endsWith(".csv") && 
            !filename.endsWith(".xlsx") && 
            !filename.endsWith(".xls")) {
            throw new InvalidFileTypeException("Invalid file extension");
        }
        
        // Check file signature (magic bytes)
        byte[] header = new byte[4];
        try (InputStream is = file.getInputStream()) {
            is.read(header);
            if (!isValidFileSignature(header, filename)) {
                throw new InvalidFileTypeException("File signature mismatch");
            }
        }
    }
    
    private boolean isValidFileSignature(byte[] header, String filename) {
        if (filename.endsWith(".xlsx")) {
            // XLSX signature: PK (50 4B 03 04)
            return header[0] == 0x50 && header[1] == 0x4B;
        }
        return true; // CSV has no signature
    }
}
```

**2. File Size Limits**

```properties
# application.properties
spring.servlet.multipart.max-file-size=500MB
spring.servlet.multipart.max-request-size=500MB
```

**3. Virus Scanning (Optional)**

```java
@Service
public class AntivirusService {
    
    public void scanFile(File file) throws VirusDetectedException {
        // Integrate with ClamAV or similar
        ProcessBuilder pb = new ProcessBuilder(
            "clamscan", "--no-summary", file.getAbsolutePath()
        );
        
        try {
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 1) {
                throw new VirusDetectedException("Virus detected in file");
            }
        } catch (Exception e) {
            throw new RuntimeException("Antivirus scan failed", e);
        }
    }
}
```



### 8.2 Data Privacy & Access Control

**1. Role-based Access (dựa trên bảng users, role, user_role)**

```java
@RestController
@RequestMapping("/api/import")
public class ImportController {
    
    @Autowired
    private UserService userService;
    
    // Chỉ ADMIN và REVIEWER mới được import
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
    @PostMapping("/start")
    public ResponseEntity<ImportJobResponse> startImport(
        @RequestBody ImportConfigRequest config,
        Authentication authentication
    ) {
        User currentUser = userService.getUserByEmail(authentication.getName());
        // Process import with user context
        return ResponseEntity.ok(importService.startImport(config, currentUser));
    }
    
    // Tất cả các role đều xem được job status
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<ImportJobResponse> getJobStatus(
        @PathVariable Long jobId,
        Authentication authentication
    ) {
        User currentUser = userService.getUserByEmail(authentication.getName());
        
        // Check if user has permission to view this job
        ImportJob job = importJobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        
        if (!job.getCreatedBy().equals(currentUser) && 
            !userService.hasRole(currentUser, "ADMIN")) {
            throw new AccessDeniedException("You don't have permission to view this job");
        }
        
        return ResponseEntity.ok(importService.getJobStatus(jobId));
    }
    
    // Chỉ ADMIN hoặc job owner mới được cancel
    @PostMapping("/jobs/{jobId}/cancel")
    public ResponseEntity<Void> cancelJob(
        @PathVariable Long jobId,
        Authentication authentication
    ) {
        User currentUser = userService.getUserByEmail(authentication.getName());
        ImportJob job = importJobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        
        if (!job.getCreatedBy().equals(currentUser) && 
            !userService.hasRole(currentUser, "ADMIN")) {
            throw new AccessDeniedException("You don't have permission to cancel this job");
        }
        
        importService.cancelJob(jobId);
        return ResponseEntity.ok().build();
    }
}
```

**2. Audit Logging (sử dụng bảng users hiện có)**

```java
@Service
public class ImportAuditService {
    
    @Autowired
    private ImportJobRepository importJobRepository;
    
    /**
     * Log được lưu trực tiếp trong ImportJob entity
     * - created_by: user thực hiện import
     * - created_at: thời gian tạo job
     * - configuration: JSON chứa config chi tiết
     */
    public void logImportStart(ImportJob job) {
        // Job đã có thông tin user và timestamp
        // Additional logging có thể thêm vào system log
        logger.info("Import job {} started by user {} (ID: {})",
            job.getId(),
            job.getCreatedBy().getName(),
            job.getCreatedBy().getId());
    }
    
    public void logImportComplete(ImportJob job) {
        logger.info("Import job {} completed. Stats: {} success, {} failed, {} duplicates",
            job.getId(),
            job.getSuccessfulRows(),
            job.getFailedRows(),
            job.getDuplicateRows());
    }
    
    /**
     * Có thể tạo bảng audit_log riêng nếu cần tracking chi tiết hơn
     */
}

// Optional: Create separate audit log table
@Entity
@Table(name = "import_audit_log")
public class ImportAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "import_job_id")
    private ImportJob importJob;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    private String action; // START, COMPLETE, CANCEL, FAIL
    private String ipAddress;
    private String userAgent;
    
    @Column(columnDefinition = "TEXT")
    private String details;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
```

**3. Sensitive Data Masking (theo cấu trúc Patient)**

```java
@Service
public class DataMaskingService {
    
    /**
     * Mask National ID (CMND/CCCD)
     * Example: 001234567890 -> ***********890
     */
    public String maskNationalId(String nationalId) {
        if (nationalId == null || nationalId.length() < 3) {
            return "***";
        }
        int visibleChars = 3;
        String masked = "*".repeat(nationalId.length() - visibleChars);
        return masked + nationalId.substring(nationalId.length() - visibleChars);
    }
    
    /**
     * Mask Health Insurance Number
     * Example: DN1234567890123 -> **************123
     */
    public String maskHealthInsuranceNo(String healthInsuranceNo) {
        if (healthInsuranceNo == null || healthInsuranceNo.length() < 3) {
            return "***";
        }
        int visibleChars = 3;
        String masked = "*".repeat(healthInsuranceNo.length() - visibleChars);
        return masked + healthInsuranceNo.substring(healthInsuranceNo.length() - visibleChars);
    }
    
    /**
     * Mask Phone Number
     * Example: 0987654321 -> ******4321
     */
    public String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "***";
        }
        int visibleChars = 4;
        String masked = "*".repeat(phoneNumber.length() - visibleChars);
        return masked + phoneNumber.substring(phoneNumber.length() - visibleChars);
    }
    
    /**
     * Mask patient data trong preview response
     */
    public PreviewRow maskPreviewRow(PreviewRow row) {
        Map<String, Object> data = row.getData();
        
        if (data.containsKey("nationalId")) {
            data.put("nationalId", maskNationalId((String) data.get("nationalId")));
        }
        
        if (data.containsKey("healthInsuranceNo")) {
            data.put("healthInsuranceNo", maskHealthInsuranceNo((String) data.get("healthInsuranceNo")));
        }
        
        if (data.containsKey("phoneNumber")) {
            data.put("phoneNumber", maskPhoneNumber((String) data.get("phoneNumber")));
        }
        
        return row;
    }
    
    /**
     * Apply masking to patient entity for API response
     */
    public PatientResponse maskPatientResponse(Patient patient) {
        PatientResponse response = new PatientResponse();
        response.setId(patient.getId());
        response.setFullName(patient.getFullName());
        response.setDateOfBirth(patient.getDateOfBirth());
        response.setGender(patient.getGender());
        
        // Mask sensitive fields
        response.setNationalId(maskNationalId(patient.getNationalId()));
        response.setHealthInsuranceNo(maskHealthInsuranceNo(patient.getHealthInsuranceNo()));
        response.setPhoneNumber(maskPhoneNumber(patient.getPhoneNumber()));
        
        // Address có thể giữ nguyên hoặc mask tùy yêu cầu
        response.setAddress(patient.getAddress());
        
        return response;
    }
}
```

---



## 9. Testing Strategy



### 9.1 Unit Tests

```java
@SpringBootTest
public class ImportServiceTest {
    
    @Test
    public void testValidateValidRow() {
        Map<String, String> row = Map.of(
            "fullName", "Nguyen Van A",
            "dateOfBirth", "1990-01-01",
            "gender", "M"
        );
        
        RowValidationResult result = validationService.validateRow(row, mappings);
        
        assertTrue(result.isValid());
        assertEquals(0, result.getErrors().size());
    }
    
    @Test
    public void testValidateInvalidDateFormat() {
        Map<String, String> row = Map.of(
            "fullName", "Nguyen Van A",
            "dateOfBirth", "01/01/1990", // Wrong format
            "gender", "M"
        );
        
        RowValidationResult result = validationService.validateRow(row, mappings);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.getField().equals("dateOfBirth")));
    }
}
```



### 9.2 Integration Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
public class ImportIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    public void testCompleteImportFlow() throws Exception {
        // 1. Upload file
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            "fullName,dateOfBirth,gender\nNguyen Van A,1990-01-01,M".getBytes()
        );
        
        MvcResult uploadResult = mockMvc.perform(
            multipart("/api/import/upload")
                .file(file)
                .param("sourceSystemId", "1")
        )
        .andExpect(status().isOk())
        .andReturn();
        
        // 2. Start import
        String jobId = // extract from uploadResult
        
        mockMvc.perform(
            post("/api/import/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createImportConfig(jobId))
        )
        .andExpect(status().isOk());
        
        // 3. Wait and check status
        Thread.sleep(2000);
        
        mockMvc.perform(get("/api/import/jobs/" + jobId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"));
    }
}
```

---



## 10. Deployment & Monitoring



### 10.1 Configuration

```yaml
# application.yml
import:
  max-file-size: 500MB
  batch-size: 100
  async:
    core-pool-size: 4
    max-pool-size: 8
    queue-capacity: 100
  duplicate-detection:
    threshold: 95
    algorithm: fuzzy-match
  storage:
    temp-dir: /var/mpi/import/temp
    archive-dir: /var/mpi/import/archive
```



### 10.2 Monitoring Metrics

```java
@Component
public class ImportMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public void recordImportJobCompleted(ImportJob job) {
        Counter.builder("import.jobs.completed")
            .tag("status", job.getStatus().name())
            .tag("source", job.getSourceSystem().getName())
            .register(meterRegistry)
            .increment();
        
        Timer.builder("import.job.duration")
            .tag("source", job.getSourceSystem().getName())
            .register(meterRegistry)
            .record(
                Duration.between(job.getStartedAt(), job.getCompletedAt())
            );
        
        Gauge.builder("import.job.success.rate", () -> 
            calculateSuccessRate(job)
        )
        .register(meterRegistry);
    }
}
```

---



## 11. Future Enhancements



### 11.1 Planned Features

1. **Scheduled Imports**
  - Cron-based automatic imports
  - FTP/SFTP file pickup
  - Email attachment processing
2. **Advanced Matching**
  - ML-based duplicate detection
  - Probabilistic matching scores
  - Manual merge interface
3. **Data Transformation**
  - Custom transformation rules
  - Data enrichment from external APIs
  - Format standardization
4. **Reporting**
  - Import history dashboard
  - Data quality metrics
  - Duplicate statistics
5. **API Integration**
  - Direct integration with EMR systems
  - HL7/FHIR message processing
  - Real-time streaming imports

---



## 12. Tổng Kết



### 12.1 Design Decisions & Rationale



#### ✅ BẮT BUỘC - Đã Fix

**1. Gọi** `saveMatchCandidate()` **trong nhánh trùng lặp của AsyncImportProcessor**

- **Lý do**: Bug chức năng - Nếu không save, duplicates sẽ không được track trong bảng `match_candidate`
- **Fix**: Thêm `deduplicationService.saveMatchCandidate()` ngay sau khi phát hiện duplicate
- **Impact**: Tất cả duplicates (score > threshold) sẽ được lưu vào `match_candidate` table để manual review sau

**2. Thêm Veto Rule giới tính vào** `calculateMatchScore()`

- **Lý do**: Đúng nghiệp vụ y tế - Nếu giới tính khác nhau, không thể là cùng 1 người
- **Fix**: Check gender đầu tiên, nếu mismatch → return 0 ngay lập tức
- **Impact**: Tránh false positive khi 2 người cùng tên + DOB nhưng khác giới tính

**3. Đổi** `DATA_MANAGER` **→** `REVIEWER`

- **Lý do**: Role name phải match với dữ liệu trong bảng `role`
- **Fix**: Sử dụng `@PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")`
- **Impact**: Spring Security @PreAuthorize sẽ chạy đúng với roles trong database

**4. Đổi** `RowStatus` **→** `ImportRowStatus` **trong ErrorReportService**

- **Lý do**: Lỗi compile - enum name không khớp với định nghĩa
- **Fix**: Sử dụng đúng tên enum `ImportRowStatus.FAILED`
- **Impact**: Code compile được



#### ✅ NÊN SỬA - Đã Implement

**5. Thêm Weight Redistribution khi thiếu field**

- **Lý do**: Fair scoring - Nếu patient thiếu CMND nhưng match 100% các field khác, vẫn nên được điểm cao
- **Implementation**: 
  - Track missing weight: `missingWeight += fieldWeight`
  - Scale score: `totalScore * 100 / availableWeight`
  - Cap at 100 để tránh overflow
- **Example**: Patient thiếu CMND (20 điểm), match perfect name + DOB + phone (65/80 = 81.25% → scale to 81.25 * 100/80 = 101.56 → cap at 100)

**6. Đổi** `findByFullNameContaining` **sang Blocking Strategy**

- **Lý do**: Performance - Với database lớn (>100k patients), LIKE query sẽ rất chậm
- **Implementation**:
  - Block by: DOB + first char
  - Block by: Exact National ID (highest priority)
  - Block by: Exact Health Insurance No
  - Block by: Phone number (last resort)
- **Impact**: Giảm số records cần so sánh từ 100,000 → ~100-500 records/query



#### 🤔 CẦN QUYẾT ĐỊNH

**7. Có bỏ hẳn Adaptive Weight Learning không?**

- **Option A: Bỏ hẳn (RECOMMENDED)** ✅
  - Dùng fixed weights: name=30, dob=25, nationalId=20, insurance=15, phone=10
  - Simple, dễ debug, dễ giải thích cho user
  - Weight version = "v1" (hardcoded)
  - **Không cần bảng** `weight_config`
- **Option B: Giữ lại (Complex)**
  - Tạo bảng `weight_config` để admin có thể adjust weights
  - Phức tạp hơn, cần UI để config
  - Machine learning để tự động optimize weights (future)

**→ QUYẾT ĐỊNH: Bỏ hẳn Adaptive Weight Learning trong Phase 1**

- Lý do: Keep it simple first, có thể thêm sau nếu cần
- Database: Không cần tạo bảng `weight_config`
- Code: Sử dụng hardcoded weights với version "v1"



#### 🎯 TÙY CHỌN - Để sau

**8. Cân nhắc bỏ WebSocket → dùng Polling đơn giản**

**WebSocket Approach (Current)**

```typescript
// Frontend: Real-time updates
this.stompClient.subscribe('/topic/import/' + jobId, (message) => {
  const progress = JSON.parse(message.body);
  this.updateProgress(progress);
});
```

**Polling Approach (Alternative - SIMPLER)** ✅ RECOMMENDED

```typescript
// Frontend: Poll every 2 seconds
const pollInterval = setInterval(() => {
  this.importService.getJobStatus(jobId).subscribe(status => {
    this.updateProgress(status);
    if (status.isComplete) {
      clearInterval(pollInterval);
    }
  });
}, 2000);
```

**So sánh:**


| Aspect      | WebSocket                                 | Polling                 |
| ----------- | ----------------------------------------- | ----------------------- |
| Complexity  | High (STOMP setup, connection management) | Low (simple HTTP calls) |
| Real-time   | Instant updates                           | 2-second delay          |
| Server Load | Low (push once)                           | Higher (poll every 2s)  |
| Reliability | Need reconnection logic                   | Auto-retry built-in     |
| Debugging   | Hard (connection issues)                  | Easy (HTTP logs)        |


**→ KHUYẾN NGHỊ: Dùng Polling cho Phase 1**

- Lý do: Simple to implement, good enough for use case
- Import job thường chạy vài phút, delay 2s không ảnh hưởng UX
- Có thể upgrade sang WebSocket sau nếu cần

**Implementation Polling:**

```java
// Backend: Simple REST endpoint
@GetMapping("/jobs/{jobId}/status")
public ResponseEntity<ImportJobStatusResponse> getJobStatus(@PathVariable Long jobId) {
    ImportJob job = importJobRepository.findById(jobId)
        .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
    
    return ResponseEntity.ok(ImportJobStatusResponse.builder()
        .jobId(job.getId())
        .status(job.getStatus())
        .progress((int) ((job.getProcessedRows() * 100.0) / job.getTotalRows()))
        .processedRows(job.getProcessedRows())
        .totalRows(job.getTotalRows())
        .successfulRows(job.getSuccessfulRows())
        .failedRows(job.getFailedRows())
        .duplicateRows(job.getDuplicateRows())
        .build());
}
```



### 12.2 Key Points

✅ **Architecture**: Microservices-ready với async processing
✅ **Scalability**: Batch processing + Redis queue (optional)
✅ **Security**: File validation, access control, audit logging
✅ **User Experience**: Real-time progress, drag & drop, preview
✅ **Error Handling**: Multiple validation levels, detailed error reports
✅ **Performance**: Optimized for large files (up to 500MB)
✅ **Database Integration**: Hoàn toàn tương thích với schema hiện tại (patient, patient_master, source_system, users, role)
✅ **Duplicate Detection**: Sử dụng match_candidate table để tracking duplicates

### 12.2 Database Schema Summary

**Existing Tables (Được sử dụng):**

- `facility` - Thông tin cơ sở y tế
- `source_system` - Hệ thống nguồn (chọn khi import)
- `patient` - Lưu bệnh nhân sau khi import (với match_status = PENDING)
- `patient_master` - Check duplicate với master records
- `users` - User thực hiện import (created_by)
- `role`, `user_role` - Phân quyền import
- `match_candidate` - Lưu duplicate candidates (score > threshold)

**New Tables (Cần tạo):**

- `import_job` - Lưu thông tin import job
- `import_job_detail` - Lưu chi tiết từng row (success/failed/duplicate)



### 12.3 Import Flow với Database

```
1. User upload file
   ↓
2. Create ImportJob record
   - source_system_id: hệ thống nguồn
   - created_by: user_id
   - status: PENDING
   ↓
3. Parse & Validate file
   ↓
4. Start async processing (status → PROCESSING)
   ↓
5. For each row:
   a. Validate data
   b. Check duplicate với patient_master
   c. If duplicate (score > threshold):
      - Save to import_job_detail (status: DUPLICATE)
      - Create match_candidate record
   d. If valid & no duplicate:
      - Create Patient record (match_status: PENDING)
      - Save to import_job_detail (status: SUCCESS)
   e. If validation failed:
      - Save to import_job_detail (status: FAILED)
   ↓
6. Complete job (status → COMPLETED)
   ↓
7. Display results:
   - Successful imports → đã tạo Patient records
   - Duplicates → cần manual review trong match_candidate
   - Failed rows → có error report để fix
```



### 12.4 Tech Stack Summary

**Frontend:**

- Angular 18 (Standalone Components)
- RxJS for reactive state management
- WebSocket for real-time progress
- xlsx.js / Papa Parse for file parsing
- Drag & Drop với visual feedback

**Backend:**

- Spring Boot 3.x
- Apache POI (Excel parsing)
- OpenCSV (CSV parsing)
- Spring Async (@Async annotation)
- WebSocket (STOMP protocol)
- MySQL/MariaDB (existing schema)

**Database:**

- Existing: patient, patient_master, source_system, users, role, match_candidate
- New: import_job, import_job_detail
- Indexes for performance optimization



### 12.5 Next Steps

**Phase 1: Backend Foundation**

1. Create database tables (import_job, import_job_detail)
2. Create entities (ImportJob, ImportJobDetail)
3. Create repositories
4. Implement file parser service (CSV, XLSX)
5. Implement validation service

**Phase 2: Core Import Logic**

1. Implement deduplication service (với patient_master)
2. Implement async import processor
3. Create import controller with REST APIs
4. Add WebSocket support for progress updates

**Phase 3: Frontend Development**

1. Create upload zone component (drag & drop)
2. Create preview component (table with validation status)
3. Create config panel (source system selector)
4. Create job list component (active jobs queue)
5. Implement WebSocket client for real-time updates

**Phase 4: Testing & Refinement**

1. Unit tests for validation & deduplication logic
2. Integration tests for complete import flow
3. Performance testing with large files
4. Security testing (file upload, access control)

**Phase 5: Optional Enhancements**

1. Scheduled imports
2. Email notifications
3. Advanced duplicate matching (ML-based)
4. Data transformation rules
5. Export templates

---

**Tài liệu này đã được cập nhật để phù hợp 100% với database schema hiện tại của dự án MPI. Sẵn sàng để bắt đầu implementation!** 🚀

---



## 📝 Implementation Checklist



### Phase 1: Backend Foundation (Week 1-2)

- [ ] Create database tables (`import_job`, `import_job_detail`)
- [ ] Create entities + repositories (với blocking strategy queries)
- [ ] Implement file parser (CSV, XLSX)
- [ ] Implement validation service
- [ ] Implement deduplication service (veto rule + weight redistribution + blocking)



### Phase 2: Core Import Logic (Week 2-3)

- [ ] Implement async import processor (có save match candidates)
- [ ] Create import controller REST APIs
- [ ] Add polling endpoint for progress
- [ ] Error handling & reporting service



### Phase 3: Frontend (Week 3-4)

- [ ] Upload zone component (drag & drop)
- [ ] Preview component with validation
- [ ] Config panel (source system selector)
- [ ] Job list with polling logic
- [ ] Error report download



### Phase 4: Testing (Week 4)

- [ ] Unit tests (validation, deduplication với veto rule)
- [ ] Integration tests (end-to-end import flow)
- [ ] Performance tests (large files, blocking strategy)
- [ ] Security tests (file upload, REVIEWER role)

---



## 📊 Quick Reference: Database Tables



### Existing Tables (Được sử dụng)

- `facility` - Cơ sở y tế
- `source_system` - Hệ thống nguồn (chọn khi import)
- `patient` - Lưu bệnh nhân sau import (match_status = PENDING)
- `patient_master` - Check duplicate với master records
- `users` - User thực hiện import
- `role`, `user_role` - Phân quyền (ADMIN, REVIEWER)
- `match_candidate` - Lưu duplicate candidates



### New Tables (Cần tạo)

- `import_job` - Job info
- `import_job_detail` - Chi tiết từng row



### Key Indexes

```sql
-- Duplicate detection (blocking strategy)
CREATE INDEX idx_patient_fullname ON patient(full_name);
CREATE INDEX idx_patient_dob_gender ON patient(date_of_birth, gender);
CREATE INDEX idx_patient_national_id ON patient(national_id);
CREATE INDEX idx_master_dob ON patient_master(date_of_birth);
CREATE INDEX idx_master_national_id ON patient_master(national_id);

-- Import job queries
CREATE INDEX idx_import_job_status ON import_job(status, created_at);
CREATE INDEX idx_import_detail_job ON import_job_detail(import_job_id, status);
```

---



## 🎯 Tech Stack Summary

**Frontend:** Angular 18, RxJS, Polling (2s interval), xlsx.js

**Backend:** Spring Boot 3.x, Apache POI, OpenCSV, Spring Async, MySQL

**Key Algorithms:**

- Veto Rule: Gender mismatch → score = 0
- Weight Redistribution: Fair scoring with missing fields
- Blocking Strategy: DOB + first char, National ID, Insurance No, Phone
- Fixed Weights: name=30, dob=25, nationalId=20, insurance=15, phone=10

---

**Document prepared by:** AI Assistant  
**Reviewed by:** Project Team  
**Approved for implementation:** ✅ Yes