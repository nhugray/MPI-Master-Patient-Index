# 🚀 KẾ HOẠCH HÀNH ĐỘNG - PATIENT IMPORT FEATURE

> **Ngày:** 21/09/2026  
> **Dự án:** Master Patient Index (MPI)  
> **Trạng thái:** 35% hoàn thành - Cần triển khai ngay

---

## ⚠️ CRITICAL BLOCKERS - XỬ LÝ NGAY

### 🔴 Priority 0: Missing Core Entities (2 ngày)

#### 1. Tạo PatientMaster Entity
**File:** `be/mpi/demo/src/main/java/com/mpi/demo/entity/PatientMaster.java`

```java
package com.mpi.demo.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.mpi.demo.constant.GenderEnum;
import com.mpi.demo.constant.PatientStatusEnum;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "patient_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientMaster {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "enterprise_id", unique = true, nullable = false, length = 30)
    private String enterpriseId; // Format: EMPI-2026-000001
    
    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;
    
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private GenderEnum gender;
    
    @Column(name = "national_id", length = 20)
    private String nationalId;
    
    @Column(name = "health_insurance_no", length = 20)
    private String healthInsuranceNo;
    
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;
    
    @Column(name = "address", length = 500)
    private String address;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    @Builder.Default
    private PatientStatusEnum status = PatientStatusEnum.ACTIVE;
    
    @Column(name = "merged_into_id")
    private Long mergedIntoId; // FK to another patient_master if merged
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
```

**PatientStatusEnum:** `be/mpi/demo/src/main/java/com/mpi/demo/constant/PatientStatusEnum.java`
```java
package com.mpi.demo.constant;

public enum PatientStatusEnum {
    ACTIVE,
    INACTIVE,
    MERGED
}
```

---

#### 2. Tạo MatchCandidate Entity
**File:** `be/mpi/demo/src/main/java/com/mpi/demo/entity/MatchCandidate.java`

```java
package com.mpi.demo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import com.mpi.demo.constant.MatchDecisionEnum;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "match_candidate")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchCandidate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_master_id", nullable = false)
    private PatientMaster candidateMaster;
    
    @Column(name = "match_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal matchScore;
    
    @Column(name = "score_breakdown", columnDefinition = "JSON")
    private String scoreBreakdown; // {"name":30,"dob":25,"nationalId":0,...}
    
    @Column(name = "weight_version", nullable = false, length = 20)
    @Builder.Default
    private String weightVersion = "v1";
    
    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 20)
    @Builder.Default
    private MatchDecisionEnum decision = MatchDecisionEnum.PENDING;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;
    
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
```

**MatchDecisionEnum:** `be/mpi/demo/src/main/java/com/mpi/demo/constant/MatchDecisionEnum.java`
```java
package com.mpi.demo.constant;

public enum MatchDecisionEnum {
    PENDING,
    AUTO_APPROVED,
    MANUAL_APPROVED,
    REJECTED
}
```

---

#### 3. Tạo User Entity (nếu chưa có)
**File:** `be/mpi/demo/src/main/java/com/mpi/demo/entity/User.java`

```java
package com.mpi.demo.entity;

import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.mpi.demo.constant.GenderEnum;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 255)
    private String name;
    
    @Column(unique = true, nullable = false, length = 255)
    private String email;
    
    @Column(nullable = false, length = 255)
    private String password;
    
    @Column(length = 500)
    private String address;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private GenderEnum gender;
    
    @Column(length = 500)
    private String avatar;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
```

---

### 🔴 Priority 1: Repositories (1 ngày)

#### 1. PatientMasterRepository
**File:** `be/mpi/demo/src/main/java/com/mpi/demo/repository/PatientMasterRepository.java`

```java
package com.mpi.demo.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.mpi.demo.constant.PatientStatusEnum;
import com.mpi.demo.entity.PatientMaster;

@Repository
public interface PatientMasterRepository extends JpaRepository<PatientMaster, Long> {
    
    // Blocking Strategy Queries
    
    // Block 1: DOB + First character of name (for blocking strategy)
    @Query("SELECT pm FROM PatientMaster pm WHERE " +
           "pm.dateOfBirth = ?1 AND " +
           "UPPER(SUBSTRING(pm.fullName, 1, 1)) = ?2 AND " +
           "pm.status = 'ACTIVE'")
    List<PatientMaster> findByDateOfBirthAndFullNameStartingWith(LocalDate dob, String firstChar);
    
    // Block 2: Exact National ID (highest priority)
    @Query("SELECT pm FROM PatientMaster pm WHERE " +
           "pm.nationalId = ?1 AND pm.status = ?2")
    Optional<PatientMaster> findByNationalIdAndStatus(String nationalId, PatientStatusEnum status);
    
    default Optional<PatientMaster> findActiveByNationalId(String nationalId) {
        return findByNationalIdAndStatus(nationalId, PatientStatusEnum.ACTIVE);
    }
    
    // Block 3: Exact Health Insurance Number
    @Query("SELECT pm FROM PatientMaster pm WHERE " +
           "pm.healthInsuranceNo = ?1 AND pm.status = ?2")
    Optional<PatientMaster> findByHealthInsuranceNoAndStatus(String healthInsuranceNo, PatientStatusEnum status);
    
    default Optional<PatientMaster> findActiveByHealthInsuranceNo(String healthInsuranceNo) {
        return findByHealthInsuranceNoAndStatus(healthInsuranceNo, PatientStatusEnum.ACTIVE);
    }
    
    // Block 4: Phone Number
    @Query("SELECT pm FROM PatientMaster pm WHERE " +
           "pm.phoneNumber = ?1 AND pm.status = ?2")
    List<PatientMaster> findByPhoneNumberAndStatus(String phoneNumber, PatientStatusEnum status);
    
    default List<PatientMaster> findActiveByPhoneNumber(String phoneNumber) {
        return findByPhoneNumberAndStatus(phoneNumber, PatientStatusEnum.ACTIVE);
    }
    
    // Find by enterprise ID
    Optional<PatientMaster> findByEnterpriseId(String enterpriseId);
    
    // Get next enterprise ID number
    @Query("SELECT MAX(pm.id) FROM PatientMaster pm")
    Long getMaxId();
}
```

---

#### 2. MatchCandidateRepository
**File:** `be/mpi/demo/src/main/java/com/mpi/demo/repository/MatchCandidateRepository.java`

```java
package com.mpi.demo.repository;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.mpi.demo.constant.MatchDecisionEnum;
import com.mpi.demo.entity.MatchCandidate;

@Repository
public interface MatchCandidateRepository extends JpaRepository<MatchCandidate, Long> {
    
    // Find candidates by patient
    List<MatchCandidate> findByPatientId(Long patientId);
    
    // Find pending candidates
    List<MatchCandidate> findByDecisionOrderByCreatedAtDesc(MatchDecisionEnum decision);
    
    // Find high score pending candidates
    @Query("SELECT mc FROM MatchCandidate mc WHERE " +
           "mc.matchScore >= ?1 AND " +
           "mc.decision = 'PENDING' " +
           "ORDER BY mc.matchScore DESC")
    List<MatchCandidate> findHighScorePendingCandidates(BigDecimal minScore);
    
    // Count pending reviews
    @Query("SELECT COUNT(mc) FROM MatchCandidate mc WHERE mc.decision = 'PENDING'")
    long countPendingReviews();
}
```

---

### 🔴 Priority 2: Dependencies trong pom.xml (0.25 ngày)

**File:** `be/mpi/demo/pom.xml`

Thêm vào section `<dependencies>`:

```xml
<!-- Apache POI for Excel parsing -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>

<!-- OpenCSV for CSV parsing -->
<dependency>
    <groupId>com.opencsv</groupId>
    <artifactId>opencsv</artifactId>
    <version>5.9</version>
</dependency>

<!-- Commons Text for Levenshtein Distance (Optional, có thể tự implement) -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-text</artifactId>
    <version>1.11.0</version>
</dependency>
```

Sau đó chạy:
```bash
cd be/mpi/demo
mvn clean install
```

---

### 🔴 Priority 3: Database Tables (0.5 ngày)

Chạy SQL script sau trong MySQL:

```sql
USE `mpi_system`;

-- Bảng import_job
CREATE TABLE IF NOT EXISTS `import_job` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bảng import_job_detail
CREATE TABLE IF NOT EXISTS `import_job_detail` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes for performance
CREATE INDEX `idx_import_job_status` ON `import_job` (`status`, `created_at`);
CREATE INDEX `idx_import_job_source` ON `import_job` (`source_system_id`, `created_at`);
CREATE INDEX `idx_import_detail_job` ON `import_job_detail` (`import_job_id`, `status`);

-- Indexes for duplicate detection (blocking strategy)
CREATE INDEX `idx_patient_master_fullname` ON `patient_master`(`full_name`);
CREATE INDEX `idx_patient_master_dob` ON `patient_master`(`date_of_birth`);
CREATE INDEX `idx_patient_master_national_id` ON `patient_master`(`national_id`);
CREATE INDEX `idx_patient_master_health_insurance` ON `patient_master`(`health_insurance_no`);
CREATE INDEX `idx_patient_master_phone` ON `patient_master`(`phone_number`);
CREATE INDEX `idx_patient_master_status` ON `patient_master`(`status`);

-- Composite index for fast blocking
CREATE INDEX `idx_patient_master_dob_name` ON `patient_master`(`date_of_birth`, `full_name`(10));
```

---

## 📋 ROADMAP CHI TIẾT

### Tuần 1: Foundation (Ngày 1-5)

#### Ngày 1-2: Entities & Repositories
- [x] Đọc tài liệu phân tích
- [ ] Tạo PatientMaster entity + PatientStatusEnum
- [ ] Tạo MatchCandidate entity + MatchDecisionEnum
- [ ] Tạo User entity (nếu chưa có)
- [ ] Tạo PatientMasterRepository với blocking queries
- [ ] Tạo MatchCandidateRepository
- [ ] Test repositories với unit tests

#### Ngày 3: Dependencies & Database
- [ ] Thêm Apache POI, OpenCSV vào pom.xml
- [ ] Run mvn clean install
- [ ] Chạy SQL tạo import_job, import_job_detail tables
- [ ] Tạo indexes cho performance
- [ ] Verify tables với MySQL Workbench

#### Ngày 4-5: FileParserService
- [ ] Tạo `service/file/FileParserService.java`
- [ ] Implement parseCSV() với OpenCSV
- [ ] Implement parseExcel() với Apache POI
- [ ] Implement detectColumns()
- [ ] Test với sample CSV/Excel files

### Tuần 2: Core Services (Ngày 6-10)

#### Ngày 6-7: ValidationService
- [ ] Tạo `service/validation/ValidationService.java`
- [ ] Implement validateRow()
- [ ] Business rules: required fields, date format, phone format
- [ ] Tạo DTO: RowValidationResult, ValidationError
- [ ] Unit tests

#### Ngày 8-10: DeduplicationService ⭐ QUAN TRỌNG NHẤT
- [ ] Tạo `service/deduplication/DeduplicationService.java`
- [ ] Implement Blocking Strategy (4 passes):
  - Pass 1: DOB + first char
  - Pass 2: National ID
  - Pass 3: Health Insurance
  - Pass 4: Phone number
- [ ] Implement calculateMatchScore():
  - Veto rule: gender mismatch → 0
  - Weight redistribution khi thiếu field
  - Weighted scoring (name=30, dob=25, nationalId=20, insurance=15, phone=10)
- [ ] Implement normalizeVietnameseName():
  - Loại bỏ dấu tiếng Việt
  - Lowercase + trim
- [ ] Implement Levenshtein Distance
- [ ] Implement calculatePhoneSimilarity()
- [ ] Implement saveMatchCandidate()
- [ ] Implement Strong Identifier Rule:
  ```
  if (score >= 92) {
      if (hasStrongIdentifier || score >= 98) {
          AUTO_MATCH
      } else {
          NEEDS_REVIEW
      }
  }
  ```
- [ ] Tạo DTO: DuplicationResult
- [ ] Unit tests đầy đủ (rất quan trọng!)

### Tuần 3: Async Processing & APIs (Ngày 11-15)

#### Ngày 11-12: AsyncImportProcessor
- [ ] Tạo `config/AsyncConfig.java` (thread pool setup)
- [ ] Tạo `service/async/AsyncImportProcessor.java`
- [ ] Implement processImportJob() với:
  - Batch processing (100 rows/batch)
  - @Async annotation
  - Progress tracking
  - Error handling
  - Save ImportJobDetail records
  - Call deduplicationService.findDuplicates()
  - Call deduplicationService.saveMatchCandidate()
- [ ] Implement mapToPatient()
- [ ] Test với sample data

#### Ngày 13: ImportService (Orchestration)
- [ ] Tạo `service/ImportService.java`
- [ ] Implement uploadFile():
  - Call FileParserService.parse()
  - Validate first 100 rows
  - Detect columns
  - Return preview
- [ ] Implement startImport():
  - Save ImportJob record
  - Trigger AsyncImportProcessor
  - Return job ID
- [ ] Implement getJobStatus()
- [ ] Implement cancelJob()
- [ ] Implement listJobs()

#### Ngày 14-15: ImportController & DTOs
- [ ] Tạo `controller/ImportController.java`
- [ ] Endpoints:
  - POST /api/import/upload
  - POST /api/import/start
  - GET /api/import/jobs/{jobId}
  - GET /api/import/jobs
  - POST /api/import/jobs/{jobId}/cancel
  - GET /api/import/jobs/{jobId}/errors
- [ ] Tạo DTOs còn thiếu:
  - ImportConfigRequest
  - ImportProgressResponse
  - ColumnMappingResponse
- [ ] Add @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
- [ ] Test APIs với Postman

### Tuần 4-5: Frontend (Ngày 16-25)

#### Ngày 16-17: Models & Services
- [ ] Tạo `features/patient-import/models/`
  - import-job.model.ts
  - import-config.model.ts
  - file-validation-result.model.ts
  - column-mapping.model.ts
- [ ] Tạo `features/patient-import/services/`
  - file-upload.service.ts
  - import-job.service.ts
- [ ] Test services với mock data

#### Ngày 18-19: Upload & Preview Components
- [ ] Tạo `upload-zone.component.ts`
  - Drag & Drop UI
  - File validation (type, size)
  - Visual feedback
- [ ] Tạo `data-preview.component.ts`
  - Table hiển thị 50 rows
  - Color-coded validation status
  - Match score display
  - Inline error messages
- [ ] Test components riêng lẻ

#### Ngày 20-21: Config & Job List Components
- [ ] Tạo `import-config-panel.component.ts`
  - Source system selector
  - Column mapping review
  - Duplicate threshold slider
  - Skip duplicates checkbox
- [ ] Tạo `import-job-list.component.ts`
  - Active jobs queue
  - Progress bars
  - Status badges
  - Actions: Cancel, View Details
- [ ] Implement polling logic (every 2s)

#### Ngày 22-23: Main Page & Integration
- [ ] Tạo `patient-import-page.component.ts` (main container)
- [ ] Integrate tất cả components
- [ ] Tạo routing `/patient-import`
- [ ] Thêm menu item vào sidebar
- [ ] Test flow: Upload → Preview → Configure → Start → Monitor

#### Ngày 24-25: Polish & Error Handling
- [ ] Error handling đầy đủ
- [ ] Loading states
- [ ] Toast notifications
- [ ] Download error report
- [ ] Responsive design
- [ ] Accessibility (ARIA labels)

### Tuần 6: Testing & Deployment (Ngày 26-30)

#### Ngày 26-27: Unit & Integration Tests
- [ ] Backend unit tests:
  - DeduplicationService (veto rule, weight redistribution)
  - ValidationService
  - FileParserService
- [ ] Backend integration tests:
  - Upload → Parse → Validate → Start
  - Duplicate detection accuracy
  - Match candidate tracking
- [ ] Frontend unit tests:
  - Services
  - Components

#### Ngày 28: Performance Testing
- [ ] Test với file lớn (10,000+ rows)
- [ ] Test blocking strategy performance
- [ ] Memory usage monitoring
- [ ] Concurrent imports (multiple users)
- [ ] Database query optimization

#### Ngày 29: UAT & Bug Fixes
- [ ] User Acceptance Testing
- [ ] Collect feedback
- [ ] Fix critical bugs
- [ ] Edge cases testing

#### Ngày 30: Deployment
- [ ] Production database migration
- [ ] Deploy backend
- [ ] Deploy frontend
- [ ] Smoke testing in production
- [ ] Monitor logs

---

## 🎯 VALIDATION CHECKLIST

### Backend Validation
```
[ ] PatientMaster entity có đầy đủ fields theo db.md
[ ] MatchCandidate entity có scoreBreakdown JSON column
[ ] Repositories có blocking strategy queries
[ ] DeduplicationService có veto rule (gender mismatch → 0)
[ ] DeduplicationService có weight redistribution
[ ] DeduplicationService có Levenshtein distance
[ ] DeduplicationService có normalize Vietnamese name
[ ] DeduplicationService có strong identifier rule
[ ] AsyncImportProcessor call saveMatchCandidate()
[ ] ImportJob entities sync với database tables
[ ] Apache POI dependency trong pom.xml
[ ] OpenCSV dependency trong pom.xml
[ ] All indexes created cho blocking strategy
```

### Frontend Validation
```
[ ] File upload drag & drop works
[ ] CSV parsing works
[ ] Excel parsing works
[ ] Preview table shows data correctly
[ ] Validation errors display inline
[ ] Match score visible in preview
[ ] Progress bar updates every 2s (polling)
[ ] Job list shows all jobs
[ ] Can cancel running job
[ ] Can download error report
[ ] Color coding: green (>95), yellow (70-95), red (<70)
[ ] Responsive design works on mobile
```

### Integration Validation
```
[ ] End-to-end: Upload → Parse → Preview → Start → Complete
[ ] Duplicate detection works correctly
[ ] Match candidates saved in database
[ ] High score (>92) with strong identifier → AUTO_MATCH
[ ] Medium score (60-92) → NEEDS_REVIEW
[ ] Low score (<60) → NO_MATCH (new master)
[ ] Gender mismatch → score = 0
[ ] Weight redistribution với missing CCCD works
[ ] Vietnamese name normalization works
[ ] Blocking strategy reduces search space
```

---

## 📊 PROGRESS TRACKING

### Phase 1: Foundation ⏳ IN PROGRESS
- [x] Đọc tài liệu
- [ ] 0/3 Entities created
- [ ] 0/2 Repositories created
- [ ] 0/3 Dependencies added
- [ ] 0/2 Database tables created
- [ ] 0/6 Indexes created

### Phase 2: Core Services ❌ NOT STARTED
- [ ] 0/1 FileParserService
- [ ] 0/1 ValidationService
- [ ] 0/1 DeduplicationService
- [ ] 0/1 AsyncImportProcessor
- [ ] 0/1 ImportService

### Phase 3: APIs ❌ NOT STARTED
- [ ] 0/1 ImportController
- [ ] 0/6 API Endpoints
- [ ] 0/5 DTOs

### Phase 4: Frontend ❌ NOT STARTED
- [ ] 0/4 Models
- [ ] 0/2 Services
- [ ] 0/5 Components
- [ ] 0/1 Main page
- [ ] 0/1 Routing

### Phase 5: Testing ❌ NOT STARTED
- [ ] 0/10 Unit tests
- [ ] 0/5 Integration tests
- [ ] 0/4 Performance tests
- [ ] 0/1 UAT

**TỔNG TIẾN ĐỘ: 1/60 tasks = 2% of Phase 1-5**

---

## 🚨 REMINDER: CRITICAL SUCCESS FACTORS

1. **Veto Rule:** Gender mismatch MUST return score = 0
2. **Weight Redistribution:** Thiếu field phải scale score correctly
3. **Blocking Strategy:** MUST use indexed queries, không dùng LIKE '%name%'
4. **Strong Identifier Rule:** AUTO_MATCH chỉ khi có CCCD/BHYT matched HOẶC score >= 98
5. **Save Match Candidates:** MUST save vào match_candidate table khi detect duplicate
6. **Vietnamese Normalization:** Loại bỏ dấu đúng cách (à→a, ê→e, ô→o, ư→u, đ→d)
7. **Batch Processing:** Process 100 rows/batch để tránh OutOfMemory
8. **Polling:** Poll every 2 seconds, không dùng WebSocket (simplicity first)

---

**BẮT ĐẦU NGAY:** Tạo PatientMaster entity + repository đầu tiên! 🚀

*Document này là roadmap chi tiết để triển khai Patient Import Feature. Follow từng bước một cách tuần tự.*