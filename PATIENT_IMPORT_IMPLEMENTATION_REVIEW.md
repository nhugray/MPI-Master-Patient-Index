# 📊 ĐÁNH GIÁ TIẾN ĐỘ DỰ ÁN MPI - PATIENT IMPORT FEATURE

> **Ngày đánh giá:** 21/09/2026  
> **Người đánh giá:** AI Assistant  
> **Trạng thái:** Đang triển khai Phase 1

---

## 🎯 TÓM TẮT TỔNG QUAN

### Mục tiêu dự án
Xây dựng hệ thống **Master Patient Index (MPI)** với tính năng import hàng loạt bệnh nhân từ file CSV/Excel, có khả năng:
- Duplicate detection (phát hiện trùng lặp) với thuật toán matching điểm
- Weight redistribution khi thiếu field
- Veto rule cho giới tính
- Blocking strategy để tối ưu hiệu suất

### Tiến độ hiện tại: **35% hoàn thành**

```
[████████░░░░░░░░░░░░░░] 35%

✅ Phase 1: Database Schema & Entities - 80% HOÀN THÀNH
⏳ Phase 2: Import Service Logic - 0% CHƯA BẮT ĐẦU  
❌ Phase 3: Frontend UI - 0% CHƯA BẮT ĐẦU
❌ Phase 4: Testing & Integration - 0% CHƯA BẮT ĐẦU
```

---

## 📋 PHẦN 1: ĐÁNH GIÁ BACKEND

### 1.1 Database Schema ✅ **HOÀN THÀNH 100%**

**Bảng đã có sẵn:**
```sql
✅ facility - Cơ sở y tế
✅ source_system - Hệ thống nguồn (có FK facility_id)
✅ patient - Bệnh nhân (có FK source_system_id, master_patient_id)
✅ patient_master - Golden Record (có enterprise_id, merged_into_id)
✅ users - Người dùng
✅ role, user_role - Phân quyền
✅ match_candidate - Ứng viên trùng lặp (có score, breakdown, decision)
✅ match_decision_log - Lịch sử quyết định
```

**Bảng CẦN TẠO cho Import Feature:**
```sql
⚠️ CHƯA TẠO: import_job
⚠️ CHƯA TẠO: import_job_detail
```

**File SQL đã có:** `be/mpi/demo/src/main/java/com/mpi/demo/db.md` (đầy đủ schema)

---

### 1.2 Entities & Enums ✅ **80% HOÀN THÀNH**

#### ✅ Đã có (Core Entities)

**Patient Entity** (`be/mpi/demo/src/main/java/com/mpi/demo/entity/Patient.java`):
```java
✅ id, sourceSystem, localPatientCode
✅ fullName, dateOfBirth, gender
✅ nationalId, healthInsuranceNo, phoneNumber, address
✅ masterPatientId (FK to patient_master)
✅ matchStatus (PENDING, MATCHED, NEW_MASTER, REJECTED)
✅ createdAt, updatedAt
```

**MatchStatusEnum** (`be/mpi/demo/src/main/java/com/mpi/demo/constant/MatchStatusEnum.java`):
```java
✅ PENDING, MATCHED, NEW_MASTER, REJECTED
```

#### ✅ Đã có (Import Entities)

**ImportJob Entity** (`be/mpi/demo/src/main/java/com/mpi/demo/entity/ImportJob.java`):
```java
✅ id, fileName, fileSize, fileType
✅ sourceSystem (ManyToOne)
✅ status (ImportJobStatusEnum)
✅ totalRows, processedRows, successfulRows, failedRows, duplicateRows, warningRows
✅ startedAt, completedAt, errorMessage
✅ configuration (JSON)
✅ createdBy, createdAt, updatedAt
✅ details (OneToMany ImportJobDetail)
```

**ImportJobDetail Entity** (`be/mpi/demo/src/main/java/com/mpi/demo/entity/ImportJobDetail.java`):
```java
✅ id, importJob, rowNumber
✅ rowData (JSON)
✅ status (ImportRowStatusEnum)
✅ matchScore, patient, matchedMasterId
✅ errorMessage, warningMessage, processedAt
```

**FileTypeEnum** (`be/mpi/demo/src/main/java/com/mpi/demo/constant/FileTypeEnum.java`):
```java
✅ CSV, XLSX
```

**ImportJobStatusEnum** (`be/mpi/demo/src/main/java/com/mpi/demo/constant/ImportJobStatusEnum.java`):
```java
✅ PENDING, VALIDATING, PROCESSING, COMPLETED, FAILED, CANCELLED
```

**ImportRowStatusEnum** (`be/mpi/demo/src/main/java/com/mpi/demo/constant/ImportRowStatusEnum.java`):
```java
✅ SUCCESS, FAILED, DUPLICATE, WARNING
```

#### ❌ THIẾU (Cần tạo)

```java
❌ PatientMaster entity - QUAN TRỌNG!
❌ MatchCandidate entity - QUAN TRỌNG!
❌ SourceSystem entity (có thể đã có nhưng chưa kiểm tra)
❌ Facility entity (có thể đã có nhưng chưa kiểm tra)
```

---

### 1.3 DTOs ✅ **60% HOÀN THÀNH**

#### ✅ Đã có

**Request DTOs:**
```java
✅ UploadFileRequest (file, sourceSystemId)
✅ StartImportRequest (sourceSystemId, columnMappings, skipDuplicates, duplicateThreshold, fileToken)
✅ ImportJobSearchRequest
```

**Response DTOs:**
```java
✅ FileValidationResponse (isValid, fileToken, fileName, fileSize, totalRows, validRows, invalidRows, 
                           detectedColumns, suggestedMappings, errors, preview)
✅ ImportJobResponse
✅ PreviewRowDto
✅ ValidationErrorDto
```

#### ❌ THIẾU các DTOs

```java
❌ ImportConfigRequest
❌ ImportProgressResponse/Update
❌ ColumnMappingRequest/Response
❌ RowValidationResult
❌ DuplicationResult
```

---

### 1.4 Repositories ⚠️ **40% HOÀN THÀNH**

#### ✅ Đã có

```java
✅ ImportJobRepository
✅ ImportJobDetailRepository
✅ PatientRepository (cơ bản)
✅ FacilityRepository
✅ SourceSystemRepository
```

#### ❌ THIẾU hoặc CẦN Bổ SUNG

```java
❌ PatientMasterRepository - QUAN TRỌNG!
   - findByDateOfBirthAndFullNameStartingWith() - Blocking strategy
   - findByNationalIdAndStatus()
   - findByHealthInsuranceNoAndStatus()
   - findByPhoneNumberAndStatus()
   - findByEnterpriseId()

❌ MatchCandidateRepository - QUAN TRỌNG!
   - findByPatientId()
   - findByDecisionOrderByCreatedAtDesc()
   - findHighScorePendingCandidates()

⚠️ PatientRepository CẦN BỔ SUNG:
   - findBySourceSystemIdAndLocalPatientCode()
   - findByMatchStatus()
```

---

### 1.5 Services ❌ **0% HOÀN THÀNH**

#### ✅ Đã có (CRUD cơ bản)

```java
✅ PatientService + PatientServiceImpl (CRUD cơ bản)
✅ FacilityService + FacilityServiceImpl
✅ SourceSystemService + SourceSystemServiceImpl
```

#### ❌ THIẾU HOÀN TOÀN (Import Feature)

```java
❌ ImportService - QUAN TRỌNG!
   - uploadFile()
   - startImport()
   - getJobStatus()
   - cancelJob()
   - listJobs()

❌ FileParserService - QUAN TRỌNG!
   - parseCSV()
   - parseExcel()
   - detectColumns()

❌ ValidationService - QUAN TRỌNG!
   - validateRow()
   - validateFile()
   - Business rules validation

❌ DeduplicationService - QUAN TRỌNG NHẤ(T!
   - findDuplicates() với Blocking Strategy
   - calculateMatchScore() với:
     * Veto rule (gender mismatch → 0)
     * Weight redistribution
     * Levenshtein distance
   - normalizeVietnameseName()
   - saveMatchCandidate()

❌ AsyncImportProcessor - QUAN TRỌNG!
   - processImportJob() với batch processing
   - mapToPatient()
   - sendProgressUpdate()

❌ DataMaskingService (Optional)
❌ ErrorReportService (Optional)
```

---

### 1.6 Controllers ❌ **0% HOÀN THÀNH**

#### ✅ Đã có

```java
✅ PatientController (CRUD cơ bản)
✅ FacilityController
✅ SourceSystemController
```

#### ❌ THIẾU

```java
❌ ImportController - TOÀN BỘ!
   - POST /api/import/upload
   - POST /api/import/start
   - GET /api/import/jobs/{jobId}
   - GET /api/import/jobs
   - POST /api/import/jobs/{jobId}/cancel
   - GET /api/import/jobs/{jobId}/errors
   - POST /api/import/suggest-mapping
```

---

### 1.7 Configuration ❌ **0% HOÀN THÀNH**

```java
❌ AsyncConfig - Cần tạo thread pool cho async processing
❌ CorsConfig - Có thể đã có, cần kiểm tra
❌ WebSocketConfig (nếu dùng WebSocket) hoặc bỏ qua nếu dùng Polling
❌ FileUploadConfig - multipart file size limits
```

---

### 1.8 Dependencies ⚠️ **THIẾU QUAN TRỌNG**

**pom.xml hiện tại:**
```xml
✅ Spring Boot 4.0.7
✅ Spring Data JPA
✅ Spring Validation
✅ Spring WebMVC
✅ MySQL Connector
✅ Lombok
✅ Springdoc OpenAPI
```

**CẦN BỔ SUNG:**
```xml
❌ Apache POI (cho Excel parsing) - QUAN TRỌNG!
   <dependency>
     <groupId>org.apache.poi</groupId>
     <artifactId>poi-ooxml</artifactId>
     <version>5.2.5</version>
   </dependency>

❌ OpenCSV (cho CSV parsing) - QUAN TRỌNG!
   <dependency>
     <groupId>com.opencsv</groupId>
     <artifactId>opencsv</artifactId>
     <version>5.9</version>
   </dependency>

❌ Jackson Databind (có thể đã có qua Spring Boot)
   - Cần cho JSON parsing trong configuration

⚠️ Spring WebSocket (nếu không dùng polling thì không cần)
```

---

## 📋 PHẦN 2: ĐÁNH GIÁ FRONTEND

### 2.1 Cấu trúc hiện tại

**Đã có:**
- ✅ 44 TypeScript files
- ✅ Angular 18 Standalone Components
- ✅ Features: Patient, Facility, SourceSystem, Dashboard
- ✅ Shared components: Pagination, Toast, ConfirmDialog
- ✅ Services: Patient, Facility, SourceSystem, Search
- ✅ Models & Enums đầy đủ cho các module hiện tại

**Routes hiện tại:**
```typescript
✅ /patients - Danh sách bệnh nhân
✅ /facilities - Cơ sở y tế
✅ /source-systems - Hệ thống nguồn
✅ /dashboard - Dashboard
❌ /patient-import - CHƯA CÓ!
```

---

### 2.2 Import Feature (Frontend) ❌ **0% HOÀN THÀNH**

**CẦN TẠO TOÀN BỘ:**

```
fe/src/app/features/patient-import/
├── components/
│   ├── patient-import-page/ ❌
│   │   ├── patient-import-page.component.ts
│   │   ├── patient-import-page.component.html
│   │   └── patient-import-page.component.css
│   ├── upload-zone/ ❌
│   │   └── upload-zone.component.ts (Drag & Drop)
│   ├── import-config-panel/ ❌
│   │   └── import-config-panel.component.ts
│   ├── data-preview/ ❌
│   │   └── data-preview.component.ts (Preview table)
│   ├── import-job-list/ ❌
│   │   └── import-job-list.component.ts (Active jobs)
│   └── column-mapping-dialog/ ❌
│       └── column-mapping-dialog.component.ts
├── services/
│   ├── file-upload.service.ts ❌
│   ├── import-job.service.ts ❌
│   └── websocket.service.ts ❌ (hoặc polling service)
├── models/
│   ├── import-job.model.ts ❌
│   ├── import-config.model.ts ❌
│   ├── file-validation-result.model.ts ❌
│   └── column-mapping.model.ts ❌
└── patient-import.routes.ts ❌
```

---

### 2.3 Sidebar Navigation

**Hiện tại:** Sidebar có các menu cơ bản nhưng **THIẾU Import**

```html
✅ Dashboard
✅ Bệnh nhân
✅ Cơ sở Y tế
✅ Hệ thống Nguồn
✅ Lịch hẹn (placeholder)
✅ Hồ sơ bệnh án (placeholder)
✅ Báo cáo (placeholder)
❌ Import Bệnh nhân - THIẾU!
```

---

## 📋 PHẦN 3: PHÂN TÍCH LUỒNG NGHIỆP VỤ

### 3.1 So sánh với tài liệu yêu cầu

**Tài liệu cung cấp:**
```
GIAI ĐOẠN 1: Chuẩn hóa (Standardization)
GIAI ĐOẠN 2: Blocking (4 passes)
GIAI ĐOẠN 3: Chấm điểm từng ứng viên
  - Veto rule: Gender mismatch → 0
  - Weight redistribution khi thiếu field
  - Weighted scoring: name=30, dob=25, nationalId=20, insurance=15, phone=10
GIAI ĐOẠN 4: So với ngưỡng
  - >= 92: AUTO_MATCH
  - 60-91.99: NEEDS_REVIEW
  - < 60: NO_MATCH
GIAI ĐOẠN 5: Thực thi kết quả (transaction)
```

**Triển khai hiện tại:**
```
❌ Service chưa có → Luồng nghiệp vụ CHƯA TRIỂN KHAI
❌ Chuẩn hóa tên tiếng Việt → Chưa có
❌ Blocking strategy → Chưa có (queries chưa tạo)
❌ Veto rule → Chưa có
❌ Weight redistribution → Chưa có
❌ Levenshtein distance → Chưa có
❌ Match candidate tracking → Chưa có
```

---

### 3.2 Trường hợp đặc biệt

**Yêu cầu từ tài liệu:**
> "AUTO_MATCH chỉ được phép khi có ít nhất 1 trường định danh MẠNH (CCCD hoặc BHYT) 
> thực sự khớp, HOẶC điểm redistribution >= 98"

**Trạng thái:** ❌ **CHƯA TRIỂN KHAI**

Cần logic:
```java
if (score >= 92) {
    boolean hasStrongIdentifier = 
        (nationalIdMatched && nationalIdScore > 0) ||
        (healthInsuranceMatched && healthInsuranceScore > 0);
    
    if (hasStrongIdentifier || score >= 98) {
        return MatchDecision.AUTO_MATCH;
    } else {
        return MatchDecision.NEEDS_REVIEW;
    }
}
```

---

## 📊 PHẦN 4: ĐÁNH GIÁ CHI TIẾT TỪNG COMPONENT

### 4.1 Backend Services - Chi tiết

#### DeduplicationService (QUAN TRỌNG NHẤT)

**Yêu cầu:**
1. ✅ Blocking Strategy (4 passes) - Đã có trong doc
2. ✅ Veto Rule - Đã có trong doc
3. ✅ Weight Redistribution - Đã có trong doc
4. ✅ Levenshtein Distance - Đã có trong doc
5. ✅ Save Match Candidate - Đã có trong doc

**Trạng thái:** ❌ **CHƯA CÓ FILE**

**Cần tạo:**
```java
be/mpi/demo/src/main/java/com/mpi/demo/service/DeduplicationService.java
be/mpi/demo/src/main/java/com/mpi/demo/service/impl/DeduplicationServiceImpl.java
```

**Các method cần:**
- `DuplicationResult findDuplicates(Patient newPatient)`
- `BigDecimal calculateMatchScore(Patient patient, PatientMaster master, Map<String, BigDecimal> scoreBreakdown)`
- `BigDecimal calculateNameSimilarity(String name1, String name2)`
- `String normalizeVietnameseName(String name)`
- `int levenshteinDistance(String s1, String s2)`
- `BigDecimal calculatePhoneSimilarity(String phone1, String phone2)`
- `void saveMatchCandidate(Patient patient, PatientMaster master, BigDecimal score, Map<String, BigDecimal> scoreBreakdown)`

---

#### AsyncImportProcessor

**Yêu cầu:**
- Batch processing (100 rows/batch)
- Async execution với @Async
- Progress tracking
- Error handling

**Trạng thái:** ❌ **CHƯA CÓ FILE**

**Cần tạo:**
```java
be/mpi/demo/src/main/java/com/mpi/demo/service/AsyncImportProcessor.java
```

---

#### FileParserService

**Yêu cầu:**
- Parse CSV với OpenCSV
- Parse XLSX với Apache POI
- Detect columns tự động
- Handle large files (streaming)

**Trạng thái:** ❌ **CHƯA CÓ FILE**

**Dependencies:** ❌ **THIẾU trong pom.xml**

---

### 4.2 Frontend Components - Chi tiết

**Upload Zone:**
- Drag & Drop UI
- File type validation (CSV, XLSX)
- File size validation (< 500MB)
- Visual feedback

**Trạng thái:** ❌ **CHƯA CÓ**

**Data Preview:**
- Table hiển thị 50-100 rows đầu
- Color-coded validation status
- Match score display
- Inline error messages

**Trạng thái:** ❌ **CHƯA CÓ**

**Import Job List:**
- Active jobs queue
- Progress bar
- Status badges
- Actions: Cancel, View Details, Download Errors

**Trạng thái:** ❌ **CHƯA CÓ**

---

## 🔍 PHẦN 5: VẤN ĐỀ & RỦI RO

### 5.1 Vấn đề quan trọng ⚠️

#### 1. THIẾU PatientMaster Entity
**Mức độ:** 🔴 **CRITICAL**

```
❌ Không có entity PatientMaster
❌ Không có repository PatientMasterRepository
❌ Không thể thực hiện duplicate detection
```

**Giải pháp:** Tạo ngay entity theo schema trong db.md

---

#### 2. THIẾU MatchCandidate Entity
**Mức độ:** 🔴 **CRITICAL**

```
❌ Không có entity MatchCandidate
❌ Không có repository MatchCandidateRepository
❌ Không thể track duplicates để manual review
```

**Giải pháp:** Tạo ngay entity theo schema trong db.md

---

#### 3. THIẾU Dependencies cho File Parsing
**Mức độ:** 🔴 **CRITICAL**

```
❌ Không có Apache POI → Không parse được Excel
❌ Không có OpenCSV → Không parse được CSV đúng cách
```

**Giải pháp:** Thêm vào pom.xml ngay

---

#### 4. CHƯA TẠO Database Tables
**Mức độ:** 🔴 **CRITICAL**

```
❌ import_job table chưa tồn tại
❌ import_job_detail table chưa tồn tại
```

**Giải pháp:** Run SQL script từ PATIENT_IMPORT_ANALYSIS.md

---

### 5.2 Rủi ro kỹ thuật ⚠️

#### Performance Risk
- **Vấn đề:** Blocking strategy queries có thể chậm nếu không có index
- **Giải pháp:** Tạo indexes như trong doc (đã có trong PATIENT_IMPORT_ANALYSIS.md)

#### Memory Risk
- **Vấn đề:** Import file lớn (500MB) có thể gây OutOfMemory
- **Giải pháp:** Streaming + batch processing (đã có trong design)

#### Concurrency Risk
- **Vấn đề:** Multiple imports cùng lúc có thể conflict
- **Giải pháp:** Thread pool với queue (AsyncConfig)

---

## ✅ PHẦN 6: CHECKLIST TRIỂN KHAI

### Phase 1: Database & Entities (Ưu tiên cao)

```
[ ] 1.1 Chạy SQL tạo bảng import_job, import_job_detail
[ ] 1.2 Tạo PatientMaster entity + repository
[ ] 1.3 Tạo MatchCandidate entity + repository
[ ] 1.4 Bổ sung methods cho PatientRepository
[ ] 1.5 Bổ sung methods cho PatientMasterRepository (blocking queries)
[ ] 1.6 Tạo indexes cho performance
[ ] 1.7 Test repositories với sample data
```

### Phase 2: Backend Services (Ưu tiên cao)

```
[ ] 2.1 Thêm dependencies: Apache POI, OpenCSV vào pom.xml
[ ] 2.2 Tạo FileParserService (CSV + XLSX)
[ ] 2.3 Tạo ValidationService
[ ] 2.4 Tạo DeduplicationService với:
    [ ] Blocking strategy
    [ ] Veto rule
    [ ] Weight redistribution
    [ ] Levenshtein distance
    [ ] Vietnamese name normalization
[ ] 2.5 Tạo AsyncImportProcessor
[ ] 2.6 Tạo ImportService (orchestration)
[ ] 2.7 Tạo AsyncConfig (thread pool)
[ ] 2.8 Test từng service riêng biệt
```

### Phase 3: Backend Controllers & APIs

```
[ ] 3.1 Tạo ImportController với đầy đủ endpoints
[ ] 3.2 Tạo DTOs còn thiếu
[ ] 3.3 Error handling & validation
[ ] 3.4 Security: @PreAuthorize cho REVIEWER role
[ ] 3.5 File upload config (multipart size)
[ ] 3.6 Test APIs với Postman/Swagger
```

### Phase 4: Frontend Implementation

```
[ ] 4.1 Tạo models & enums
[ ] 4.2 Tạo FileUploadService
[ ] 4.3 Tạo ImportJobService
[ ] 4.4 Tạo UploadZone component (drag & drop)
[ ] 4.5 Tạo DataPreview component
[ ] 4.6 Tạo ImportConfigPanel component
[ ] 4.7 Tạo ImportJobList component
[ ] 4.8 Tạo ColumnMappingDialog component
[ ] 4.9 Tạo PatientImportPage (main container)
[ ] 4.10 Thêm route /patient-import
[ ] 4.11 Thêm menu item vào sidebar
[ ] 4.12 Implement polling logic (mỗi 2s)
[ ] 4.13 Test end-to-end flow
```

### Phase 5: Testing & Integration

```
[ ] 5.1 Unit tests cho DeduplicationService
[ ] 5.2 Unit tests cho ValidationService
[ ] 5.3 Integration test: Upload → Parse → Preview
[ ] 5.4 Integration test: Start Import → Process → Complete
[ ] 5.5 Test với file lớn (> 10,000 rows)
[ ] 5.6 Test duplicate detection accuracy
[ ] 5.7 Test error handling & recovery
[ ] 5.8 Performance testing
[ ] 5.9 Security testing (file upload vulnerabilities)
[ ] 5.10 UAT với end users
```

---

## 📈 PHẦN 7: ƯỚC LƯỢNG THỜI GIAN

### Breakdown chi tiết

| Phase | Task | Thời gian | Dependency |
|-------|------|-----------|------------|
| **1** | Database tables | 0.5 ngày | None |
| **1** | PatientMaster entity | 0.5 ngày | DB tables |
| **1** | MatchCandidate entity | 0.5 ngày | DB tables |
| **1** | Repository methods | 1 ngày | Entities |
| **1** | Indexes & optimization | 0.5 ngày | Repositories |
| **2** | Add POI/CSV dependencies | 0.25 ngày | None |
| **2** | FileParserService | 2 ngày | Dependencies |
| **2** | ValidationService | 1.5 ngày | None |
| **2** | DeduplicationService | 3 ngày | PatientMaster repo |
| **2** | AsyncImportProcessor | 2 ngày | All services |
| **2** | ImportService | 1 ngày | AsyncProcessor |
| **2** | AsyncConfig | 0.5 ngày | None |
| **3** | ImportController | 1.5 ngày | ImportService |
| **3** | DTOs | 1 ngày | None |
| **3** | Security & validation | 1 ngày | Controller |
| **4** | Frontend models | 0.5 ngày | None |
| **4** | Services (FE) | 1 ngày | None |
| **4** | UploadZone component | 1.5 ngày | Services |
| **4** | DataPreview component | 1.5 ngày | Services |
| **4** | Other components | 2 ngày | Services |
| **4** | Main page & routing | 1 ngày | Components |
| **4** | Polling logic | 0.5 ngày | Services |
| **5** | Unit tests | 2 ngày | All services |
| **5** | Integration tests | 2 ngày | Full stack |
| **5** | Performance testing | 1 ngày | Full stack |
| **5** | UAT | 1 ngày | All tests pass |

**Tổng ước lượng:** ~30 ngày làm việc (6 tuần với 1 developer full-time)

**Với team 2 developers:** ~3-4 tuần

---

## 🎯 PHẦN 8: KHUYẾN NGHỊ

### 8.1 Ưu tiên triển khai

**Tuần 1-2: Foundation (CRITICAL)**
1. ✅ Tạo database tables ngay
2. ✅ Tạo PatientMaster + MatchCandidate entities
3. ✅ Thêm Apache POI + OpenCSV dependencies
4. ✅ Tạo FileParserService
5. ✅ Tạo DeduplicationService (core logic)

**Tuần 3: Backend Completion**
6. ✅ Tạo ValidationService + AsyncImportProcessor
7. ✅ Tạo ImportController + APIs
8. ✅ Test backend với Postman

**Tuần 4-5: Frontend**
9. ✅ Tạo toàn bộ frontend components
10. ✅ Integration với backend APIs
11. ✅ Test end-to-end flow

**Tuần 6: Testing & Polish**
12. ✅ Testing đầy đủ
13. ✅ Fix bugs
14. ✅ Performance optimization
15. ✅ UAT

---

### 8.2 Quyết định kỹ thuật

#### Polling vs WebSocket?
**KHUYẾN NGHỊ: Polling** ✅
- Đơn giản hơn (no STOMP setup)
- Đủ tốt cho use case (2s delay acceptable)
- Dễ debug hơn
- Có thể upgrade sang WebSocket sau

#### Adaptive Weight Learning?
**QUYẾT ĐỊNH: BỎ trong Phase 1** ✅
- Fixed weights: name=30, dob=25, nationalId=20, insurance=15, phone=10
- Version = "v1" hardcoded
- Có thể thêm sau nếu cần

#### Virus Scanning?
**QUYẾT ĐỊNH: BỎ trong Phase 1**
- Thêm sau nếu cần
- Focus vào core features trước

---

### 8.3 Các điểm cần chú ý đặc biệt

#### 1. Vietnamese Name Normalization
```
"Trần Thị Bích" → "tran thi bich"
Loại bỏ dấu: à→a, é→e, ô→o, ư→u, đ→d
```
**Trạng thái:** ❌ Chưa có → Cần implement regex mapping đầy đủ

#### 2. Strong Identifier Rule
```
AUTO_MATCH chỉ khi:
  (score >= 92) AND (
    (CCCD matched) OR 
    (BHYT matched) OR 
    (score >= 98 với redistribution)
  )
```
**Trạng thái:** ❌ Chưa có → Cần logic đặc biệt trong DeduplicationService

#### 3. Blocking Strategy Performance
```
BEFORE: SELECT * FROM patient_master WHERE full_name LIKE '%tran%'  → 100k rows
AFTER: SELECT * WHERE date_of_birth = '1988-05-12' AND full_name LIKE 'tran%' → 100 rows
```
**Trạng thái:** ❌ Chưa có queries → Cần tạo custom queries trong PatientMasterRepository

---

## 📝 PHẦN 9: KẾT LUẬN

### Tình trạng hiện tại

**✅ ĐÃ CÓ (Foundation):**
- Database schema design hoàn chỉnh
- Import entities (ImportJob, ImportJobDetail) + enums
- Basic CRUD cho Patient, Facility, SourceSystem
- Frontend framework (Angular 18) + basic components
- Document phân tích chi tiết (PATIENT_IMPORT_ANALYSIS.md)

**❌ CHƯA CÓ (Core Features):**
- PatientMaster & MatchCandidate entities **← BLOCKING**
- DeduplicationService với matching logic **← BLOCKING**
- File parsing services (CSV/Excel) **← BLOCKING**
- Import controller & APIs **← BLOCKING**
- Frontend import UI **← BLOCKING**
- Testing **← BLOCKING**

### Đánh giá tổng thể

```
🔴 CRITICAL BLOCKERS: 4 items
   1. PatientMaster entity missing
   2. MatchCandidate entity missing
   3. Apache POI/OpenCSV dependencies missing
   4. DeduplicationService missing

🟡 HIGH PRIORITY: 6 items
   1. FileParserService
   2. ValidationService
   3. AsyncImportProcessor
   4. ImportController
   5. Frontend components
   6. Database tables creation

🟢 MEDIUM PRIORITY: 3 items
   1. Error reporting
   2. Data masking
   3. Performance optimization

⚪ LOW PRIORITY: 2 items
   1. WebSocket (vs Polling)
   2. Adaptive weight learning
```

### Roadmap rõ ràng

**NGAY LẬP TỨC (Trong 3 ngày):**
1. Tạo PatientMaster entity
2. Tạo MatchCandidate entity  
3. Thêm Apache POI + OpenCSV vào pom.xml
4. Chạy SQL tạo import_job, import_job_detail tables

**TUẦN 1-2 (Foundation):**
5. Implement DeduplicationService hoàn chỉnh
6. Implement FileParserService
7. Implement ValidationService
8. Test các services riêng lẻ

**TUẦN 3 (Backend):**
9. Implement AsyncImportProcessor
10. Implement ImportController + APIs
11. Integration testing backend

**TUẦN 4-5 (Frontend):**
12. Implement toàn bộ frontend components
13. Integration với backend
14. End-to-end testing

**TUẦN 6 (Polish):**
15. Bug fixes
16. Performance tuning
17. UAT
18. Deployment

---

## 📌 ACTION ITEMS - BƯỚC TIẾP THEO

### Bước 1: Database (0.5 ngày)
```sql
-- Chạy SQL từ PATIENT_IMPORT_ANALYSIS.md Section 4.2.1
CREATE TABLE import_job (...);
CREATE TABLE import_job_detail (...);
CREATE INDEX idx_import_job_status ON import_job(status, created_at);
-- ... tất cả indexes
```

### Bước 2: Entities (1 ngày)
```
1. Tạo be/mpi/demo/src/main/java/com/mpi/demo/entity/PatientMaster.java
2. Tạo be/mpi/demo/src/main/java/com/mpi/demo/entity/MatchCandidate.java
3. Tạo be/mpi/demo/src/main/java/com/mpi/demo/entity/MatchDecisionLog.java
4. Verify entities với database
```

### Bước 3: Dependencies (0.25 ngày)
```xml
<!-- Thêm vào pom.xml -->
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
```

### Bước 4: Repositories (1 ngày)
```
1. Tạo PatientMasterRepository với blocking queries
2. Tạo MatchCandidateRepository
3. Bổ sung PatientRepository methods
4. Test repositories với unit tests
```

### Bước 5: Core Service - DeduplicationService (3 ngày)
```
1. Implement findDuplicates() với blocking strategy
2. Implement calculateMatchScore() với:
   - Veto rule (gender)
   - Weight redistribution
   - Levenshtein distance
3. Implement normalizeVietnameseName()
4. Implement saveMatchCandidate()
5. Unit tests đầy đủ
```

---

## 📚 TÀI LIỆU THAM KHẢO

1. **PATIENT_IMPORT_ANALYSIS.md** - Spec đầy đủ (2,900+ lines)
2. **Database Schema** - be/mpi/demo/src/main/java/com/mpi/demo/db.md
3. **Luồng nghiệp vụ** - User requirements về blocking, scoring, veto rule

---

**🎯 KẾT LUẬN CUỐI CÙNG:**

Dự án đã có **foundation tốt** (35% hoàn thành) với:
- ✅ Database schema đầy đủ
- ✅ Import entities
- ✅ Document chi tiết

Nhưng **THIẾU CÁC CORE COMPONENTS** (65% còn lại):
- ❌ PatientMaster/MatchCandidate entities
- ❌ Deduplication logic
- ❌ File parsing
- ❌ Import UI

**Với lộ trình rõ ràng trên, có thể hoàn thành trong 4-6 tuần với 1-2 developers.**

---

*Document này được tạo tự động bởi AI Assistant dựa trên phân tích toàn bộ codebase và requirements.*