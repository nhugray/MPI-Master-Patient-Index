# Danh sách File Backend còn thiếu - Checklist chi tiết

> Cập nhật: 21/09/2026. Danh sách đầy đủ các file cần tạo/sửa để hoàn thiện backend MPI.

## ✅ Checklist - File cần TẠO MỚI

### 1️⃣ Security & Authentication (7 files)

#### `be/mpi/demo/src/main/java/com/mpi/demo/config/SecurityConfig.java`
**Mục đích:** Cấu hình Spring Security filter chain, phân quyền endpoint
**Nội dung chính:**
- `@EnableMethodSecurity` + `@Configuration`
- Bean `SecurityFilterChain`: cho phép `/api/v1/auth/**` public, còn lại authenticated
- Bean `PasswordEncoder` (BCrypt)
- Thêm `JwtAuthenticationFilter` vào filter chain trước `UsernamePasswordAuthenticationFilter`
- Cấu hình `AuthenticationManager`

#### `be/mpi/demo/src/main/java/com/mpi/demo/security/JwtTokenProvider.java`
**Mục đích:** Sinh token, parse token, validate token
**Nội dung chính:**
- `generateToken(String email, List<String> roles)` → trả JWT string
- `extractEmail(String token)` → lấy email từ token
- `validateToken(String token)` → kiểm tra chữ ký + expiration
- Dùng `io.jsonwebtoken` (JJWT library) hoặc `spring-security-oauth2-jose`

#### `be/mpi/demo/src/main/java/com/mpi/demo/security/JwtAuthenticationFilter.java`
**Mục đích:** Filter intercept mỗi request, lấy token từ header `Authorization: Bearer <token>`, validate, set `SecurityContext`
**Nội dung chính:**
- Extends `OncePerRequestFilter`
- `doFilterInternal()`: extract token → validate → load user → set `Authentication` vào context

#### `be/mpi/demo/src/main/java/com/mpi/demo/security/CustomUserDetailsService.java`
**Mục đích:** Load user từ DB cho Spring Security
**Nội dung chính:**
- Implements `UserDetailsService`
- `loadUserByUsername(String email)` → query `UserRepository` → trả `UserDetails`

#### `be/mpi/demo/src/main/java/com/mpi/demo/repository/UserRepository.java`
**Mục đích:** Repository cho entity `User`
**Nội dung chính:**
```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

#### `be/mpi/demo/src/main/java/com/mpi/demo/service/UserService.java` + `impl/UserServiceImpl.java`
**Mục đích:** Quản lý user (đăng ký, tìm kiếm, cập nhật)
**Nội dung chính:**
- `register(CreateUserRequest)` → mã hoá password, lưu user
- `findByEmail(String email)` → Optional<User>
- Dùng `PasswordEncoder` để hash password

#### `be/mpi/demo/src/main/java/com/mpi/demo/controller/AuthController.java`
**Mục đích:** Endpoint đăng nhập, refresh token
**Nội dung chính:**
- `POST /api/v1/auth/login` → nhận `LoginRequest(email, password)`, xác thực, trả `LoginResponse(token, refreshToken, user)`
- `POST /api/v1/auth/register` → đăng ký user mới
- `POST /api/v1/auth/refresh` → dùng refresh token để lấy access token mới

#### DTO cần thêm:
- `dto/request/LoginRequest.java` (email, password)
- `dto/request/RegisterUserRequest.java` (name, email, password, address, gender)
- `dto/response/LoginResponse.java` (token, refreshToken, UserResponse)
- `dto/response/UserResponse.java` (id, name, email, gender, avatar, createdAt — không trả password)

---

### 2️⃣ Import Pipeline (10 files)

#### `be/mpi/demo/src/main/java/com/mpi/demo/service/FileStorageService.java` + `impl/FileStorageServiceImpl.java`
**Mục đích:** Lưu file upload vào disk/S3, sinh file token
**Nội dung chính:**
- `store(MultipartFile file, String originalFilename)` → lưu vào thư mục tạm (ví dụ `./uploads/temp/`), trả `fileToken` (UUID)
- `loadAsResource(String fileToken)` → đọc lại file đã lưu
- `delete(String fileToken)` → xoá file tạm sau khi import xong
- Bean `@Value("${file.upload.dir}")` từ `application.yaml`

#### `be/mpi/demo/src/main/java/com/mpi/demo/service/FileParserService.java` + `impl/FileParserServiceImpl.java`
**Mục đích:** Parse file CSV/Excel thành danh sách row + header
**Nội dung chính:**
- `parseFile(File file, FileTypeEnum type)` → trả `ParsedFileData { List<String> headers, List<Map<String, String>> rows }`
- Dùng `opencsv.CSVReader` cho CSV
- Dùng `org.apache.poi.ss.usermodel.Workbook`, `.getSheetAt(0)` cho Excel
- Xử lý encoding UTF-8 BOM cho CSV

#### `be/mpi/demo/src/main/java/com/mpi/demo/service/FileValidationService.java` + `impl/FileValidationServiceImpl.java`
**Mục đích:** Validate từng dòng dữ liệu import theo rule nghiệp vụ
**Nội dung chính:**
- `validateFile(ParsedFileData data, Long sourceSystemId)` → trả `FileValidationResponse`
- Kiểm tra: required field (tên, DOB), format DOB (dd/MM/yyyy), format SĐT (10-11 số), format CMND/CCCD (9 hoặc 12 số), trùng `localPatientCode` trong cùng source system
- Phát hiện duplicate trong file (dựa vào CMND hoặc DOB+tên)
- Trả về: `isValid`, `errors: List<ValidationErrorDto>`, `preview: List<PreviewRowDto>`, `suggestedMappings` (nếu header không chuẩn)

#### `be/mpi/demo/src/main/java/com/mpi/demo/service/ImportService.java` + `impl/ImportServiceImpl.java`
**Mục đích:** Điều phối toàn bộ luồng import
**Nội dung chính:**
- `uploadFile(MultipartFile file, UploadFileRequest req)` → lưu file, parse sơ bộ, trả `FileValidationResponse` (kèm fileToken)
- `startImport(StartImportRequest req)` → tạo `ImportJob`, gọi async processor
- `getImportJobStatus(Long jobId)` → trả `ImportJobResponse` (totalRows, processedRows, ...)
- `cancelImport(Long jobId)` → đánh dấu job cancelled, dừng processor
- `getImportJobDetails(Long jobId, Pageable)` → trả danh sách `ImportJobDetail` phân trang

#### `be/mpi/demo/src/main/java/com/mpi/demo/service/AsyncImportProcessor.java`
**Mục đích:** Xử lý import trong background thread
**Nội dung chính:**
- `@Async` method `processImport(Long importJobId)`
- Load file từ `fileToken` → parse → validate từng dòng → gọi `PatientMatchingService.findMatchCandidates` → lưu `ImportJobDetail` cho mỗi dòng
- Cập nhật `ImportJob.processedRows`, `successfulRows`, `failedRows` theo batch (commit mỗi 100 dòng chẳng hạn)
- Bắt exception, ghi vào `ImportJobDetail.errorMessage`
- Khi xong set `ImportJob.status = COMPLETED` hoặc `FAILED`

#### `be/mpi/demo/src/main/java/com/mpi/demo/config/AsyncConfig.java`
**Mục đích:** Cấu hình thread pool cho `@Async`
**Nội dung chính:**
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "taskExecutor")
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

#### `be/mpi/demo/src/main/java/com/mpi/demo/controller/ImportController.java`
**Mục đích:** Endpoint upload, preview, start, status, cancel, detail
**Nội dung chính:**
- `POST /api/v1/imports/upload` → nhận `MultipartFile`, gọi `ImportService.uploadFile`, trả `FileValidationResponse`
- `POST /api/v1/imports/start` → nhận `StartImportRequest { fileToken, sourceSystemId }`, gọi `ImportService.startImport`, trả `ImportJobResponse`
- `GET /api/v1/imports/{id}` → status job
- `GET /api/v1/imports/{id}/details` → danh sách detail có phân trang
- `POST /api/v1/imports/{id}/cancel` → cancel job
- `GET /api/v1/imports` → search jobs (theo status, dateRange, sourceSystem)
- Annotation `@PreAuthorize("hasRole('IMPORT_OPERATOR')")` trên các method

#### DTO đã có nhưng cần review:
- `UploadFileRequest` — ✅ đã có
- `StartImportRequest` — ✅ đã có
- `FileValidationResponse` — ✅ đã có
- `ImportJobResponse` — ✅ đã có
- `PreviewRowDto` — ✅ đã có
- `ValidationErrorDto` — ✅ đã có
- `ImportJobSearchRequest` — ✅ đã có

#### Cấu hình thêm trong `application.yaml`:
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
file:
  upload:
    dir: ./uploads
```

---

### 3️⃣ Match Review (2 files)

#### `be/mpi/demo/src/main/java/com/mpi/demo/controller/MatchReviewController.java`
**Mục đích:** API duyệt/từ chối match candidate
**Nội dung chính:**
- `GET /api/v1/match-candidates/pending` → gọi `PatientMatchingService.getPendingReviews(Pageable)`, trả `Page<MatchCandidateResponse>`
- `POST /api/v1/match-candidates/{id}/approve` → gọi `processMatchDecision(id, AUTO_APPROVED, currentUserId)`
- `POST /api/v1/match-candidates/{id}/reject` → gọi `processMatchDecision(id, REJECTED, currentUserId)`
- `POST /api/v1/match-candidates/{id}/create-new` → tạo `PatientMaster` mới cho patient này (không match với ai)
- Annotation `@PreAuthorize("hasRole('REVIEWER')")`

#### DTO cần thêm:
- `dto/response/MatchCandidateResponse.java` (id, patient, candidateMaster, matchScore, scoreBreakdown, decision, createdAt)

---

### 4️⃣ PatientMaster Management (2 files)

#### `be/mpi/demo/src/main/java/com/mpi/demo/service/impl/PatientMasterServiceImpl.java`
**Mục đích:** Implement interface `PatientMasterService` đã có sẵn (6 method)
**Nội dung chính:**
- `create(CreatePatientMasterRequest)` → tạo master mới
- `search(PatientMasterSearchRequest, Pageable)` → tìm kiếm master (theo tên, DOB, national ID, health insurance, enterprise ID)
- `getById(Long id)` → lấy 1 master + danh sách patient liên kết
- `merge(Long targetId, Long sourceId)` → merge 2 master, chuyển tất cả patient từ source sang target, đánh dấu source `mergedIntoId = targetId`
- `unlink(Long patientId)` → bỏ patient ra khỏi master hiện tại, set `patient.masterPatient = null`
- `update(Long id, UpdatePatientMasterRequest)` → cập nhật thông tin master

#### `be/mpi/demo/src/main/java/com/mpi/demo/controller/PatientMasterController.java`
**Mục đích:** REST API cho PatientMaster
**Nội dung chính:**
- `GET /api/v1/patient-masters` → search + pagination
- `GET /api/v1/patient-masters/{id}` → detail + danh sách patient
- `POST /api/v1/patient-masters` → tạo mới (trường hợp tạo thủ công, không qua import)
- `PUT /api/v1/patient-masters/{id}` → cập nhật
- `POST /api/v1/patient-masters/{targetId}/merge/{sourceId}` → merge
- `POST /api/v1/patient-masters/unlink-patient/{patientId}` → unlink
- Annotation `@PreAuthorize("hasRole('ADMIN')")` cho merge/unlink

#### DTO cần thêm:
- `dto/request/CreatePatientMasterRequest.java`
- `dto/request/UpdatePatientMasterRequest.java`
- `dto/request/PatientMasterSearchRequest.java`
- `dto/response/PatientMasterResponse.java` (kèm list `PatientResponse`)

---

### 5️⃣ Database Migration (Flyway)

Thêm dependency vào `pom.xml`:
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

Cấu hình `application.yaml`:
```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
```

#### `be/mpi/demo/src/main/resources/db/migration/V1__baseline_schema.sql`
**Mục đích:** Tạo tất cả bảng hiện có (facility, source_system, patient, user)
**Nội dung:** CREATE TABLE theo entity hiện tại, thêm index cho các cột search thường xuyên (facility.code, patient.localPatientCode, patient.nationalId, ...)

#### `be/mpi/demo/src/main/resources/db/migration/V2__patient_master_matching.sql`
**Mục đích:** Tạo bảng patient_master, match_candidate, sửa patient.masterPatientId thành FK
**Nội dung:**
```sql
CREATE TABLE patient_master (...);
CREATE TABLE match_candidate (...);
ALTER TABLE patient ADD CONSTRAINT fk_patient_master FOREIGN KEY (master_patient_id) REFERENCES patient_master(id);
CREATE INDEX idx_patient_master_nationalId ON patient_master(national_id);
CREATE INDEX idx_patient_master_dob ON patient_master(date_of_birth);
...
```

#### `be/mpi/demo/src/main/resources/db/migration/V3__import_tables.sql`
**Mục đích:** Tạo bảng import_job, import_job_detail
**Nội dung:**
```sql
CREATE TABLE import_job (...);
CREATE TABLE import_job_detail (...);
ALTER TABLE import_job ADD CONSTRAINT fk_import_created_by FOREIGN KEY (created_by) REFERENCES user(id);
CREATE INDEX idx_import_job_status ON import_job(status);
CREATE INDEX idx_import_job_detail_status ON import_job_detail(status);
...
```

---

## ✏️ File cần SỬA (đã tồn tại nhưng cần bổ sung logic)

### `PatientMatchingServiceImpl.java`
**Việc cần làm:**
1. Thêm Pass 4 trong `findMatchCandidates`: block theo Health Insurance Number
2. Sửa `calculateStringSimilarity`: dùng `LevenshteinDistance` từ `commons-text`, threshold cụ thể
3. Thêm **veto rule** trong `calculateMatchScore`: nếu gender khác rõ ràng (không phải null cả 2) → return 0 ngay
4. Thêm **weight redistribution**: nếu field null thì phân lại trọng số cho field có dữ liệu
5. Sửa `getAutoDecision` đúng spec: `>=92 + strong identifier → AUTO_MATCH`, `60-91 → NEEDS_REVIEW`, `<60 → NO_MATCH`
6. Implement `processMatchDecision`:
```java
MatchCandidate candidate = matchCandidateRepository.findById(candidateId)
    .orElseThrow(() -> new ResourceNotFoundException("Match candidate not found"));
candidate.setDecision(decision);
candidate.setReviewedBy(userRepository.findById(reviewedBy).orElse(null));
candidate.setReviewedAt(LocalDateTime.now());
matchCandidateRepository.save(candidate);

if (decision == MatchDecisionEnum.AUTO_APPROVED || decision == MatchDecisionEnum.APPROVED) {
    Patient patient = candidate.getPatient();
    patient.setMasterPatient(candidate.getCandidateMaster());
    patient.setMatchStatus(MatchStatusEnum.MATCHED);
    patientRepository.save(patient);
}
```

### `Patient.java` entity
**Việc cần làm:**
```java
// Đổi
private Long masterPatientId;
// Thành
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "master_patient_id")
private PatientMaster masterPatient;
```

### `PatientMaster.java` entity
**Việc cần làm:**
```java
// Thêm
@OneToMany(mappedBy = "masterPatient", cascade = CascadeType.ALL)
private List<Patient> patients = new ArrayList<>();
```

### `ImportJob.java` entity
**Việc cần làm:**
```java
// Đổi
private Long createdBy;
// Thành
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "created_by")
private User createdBy;
```

### `CorsConfig.java`
**Việc cần làm (khi lên production):**
```java
// Đổi
.allowedOriginPatterns("*")
// Thành
.allowedOrigins("https://yourdomain.com", "http://localhost:4200")
```

### `GlobalExceptionHandler.java`
**Việc cần làm:** Thêm handler cho:
```java
@ExceptionHandler(MaxUploadSizeExceededException.class)
public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
        .body(ApiResponse.ofError(413, "File quá lớn, vượt quá giới hạn cho phép"));
}

@ExceptionHandler(DataIntegrityViolationException.class)
public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiResponse.conflict("Vi phạm ràng buộc dữ liệu: " + ex.getMostSpecificCause().getMessage()));
}

@ExceptionHandler(AccessDeniedException.class)
public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiResponse.ofError(403, "Không có quyền truy cập"));
}
```

---

## 📊 Tổng kết số lượng

- **Tạo mới:** ~27 file (7 security + 10 import + 2 review + 2 master + 3 migration + 3 config)
- **Sửa:** 6 file (PatientMatchingServiceImpl, 3 entity, CorsConfig, GlobalExceptionHandler)
- **Thời gian ước tính:** 7-10 ngày làm việc cho 1 dev full-time

---

## 🎯 Thứ tự ưu tiên thực tế

1. **Cao nhất:** Security (không có auth = không dùng được thật) + Sửa entity FK
2. **Cao:** Hoàn thiện PatientMatchingService (core nghiệp vụ) + PatientMasterServiceImpl
3. **Cao:** Import pipeline (FileParser, FileValidation, ImportService, ImportController)
4. **Trung bình:** MatchReview controller, PatientMaster controller
5. **Trung bình-Thấp:** Migration Flyway (nếu chạy manual SQL được thì tạm bỏ qua)
6. **Song song:** Test (viết liên tục trong khi code feature)
