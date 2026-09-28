# Phân tích Backend MPI (be/mpi/demo) — Đã làm gì, Thiếu gì, Việc cần làm chuẩn thực tế

> Cập nhật: 21/09/2026. Tài liệu này tổng hợp lại từ khảo sát toàn bộ mã nguồn `be/mpi/demo/src/main/java/com/mpi/demo` và các doc sẵn có (`BE_IMPLEMENTATION_STATUS.md`, `PATIENT_IMPORT_ANALYSIS.md`, `NEXT_STEPS_ACTION_PLAN.md`, `PATIENT_IMPORT_IMPLEMENTATION_REVIEW.md`). Mục tiêu: một nguồn duy nhất, chính xác theo code thật, để biết cần làm gì tiếp theo.

## 1. Đã làm được (Done)

### 1.1 Module CRUD hoàn chỉnh (100%)
Ba module sau có đầy đủ Entity → Repository → Service/Impl → Controller → DTO (request/response) → Specification cho search:

- **Facility**: `Facility`, `FacilityRepository`, `FacilityService/Impl`, `FacilityController`, `CreateFacilityRequest`, `UpdateFacilityRequest`, `FacilitySearchRequest`, `FacilityResponse`, `FacilitySpecification`.
- **SourceSystem**: tương tự, có quan hệ `@ManyToOne Facility`.
- **Patient**: tương tự, có quan hệ `@ManyToOne SourceSystem`. Có cột `masterPatientId` nhưng chỉ là `Long`, chưa phải quan hệ JPA thật.

Mỗi controller có đủ 5 endpoint chuẩn REST: `GET /` (search + pagination), `GET /{id}`, `POST /`, `PUT /`, `DELETE /{id}`. Dùng `ApiResponse` + `ResultPagination` chu�ncho response wrapper.

### 1.2 Nền tảng hạ tầng
- `GlobalExceptionHandler`: xử lý `ResourceNotFoundException`, `DuplicateResourceException`, `MethodArgumentTypeMismatchException`, `HttpMessageNotReadableException`, `HttpRequestMethodNotSupportedException`, `NoHandlerFoundException`, `MethodArgumentNotValidException`, catch-all `Exception`.
- `CorsConfig`: cấu hình CORS (đang mở `*`, cần siết lại khi lên production).
- `ApiResponse` + `ResultPagination`: response envelope chuẩn.
- Dependencies đã có sẵn trong `pom.xml`: `poi-ooxml` (đọc Excel), `opencsv` (đọc CSV), `commons-text` (string similarity/Levenshtein), `springdoc-openapi` (Swagger UI).

### 1.3 Đã tạo entity/repository cho nghiệp vụ Matching & Import (nhưng service/controller thì thiếu — xem mục 2)
- `PatientMaster`, `MatchCandidate`, `ImportJob`, `ImportJobDetail`, `User` — entity đã có.
- `PatientMasterRepository`, `MatchCandidateRepository`, `ImportJobRepository`, `ImportJobDetailRepository` — repository đã có, có query blocking cho matching.
- `PatientMatchingService` + `PatientMatchingServiceImpl` — **có logic thật nhưng chưa hoàn chỉnh** (chi tiết ở mục 2.3).

## 2. Đang thiếu / chưa hoàn chỉnh (Gap)

### 2.1 Thiếu Controller (API chưa gọi được từ ngoài)
| Controller cần | Mục đích | Trạng thái |
|---|---|---|
| `ImportController` | upload file, preview, start import, xem tiến trình, cancel | **Chưa có file** |
| `MatchReviewController` | xem danh sách pending review, approve/reject match | **Chưa có file** |
| `PatientMasterController` | tìm kiếm/xem/merge/unlink hồ sơ master | **Chưa có file** |
| `AuthController` | login/logout/refresh token | **Chưa có file** |

### 2.2 Thiếu Service (nghiệp vụ chưa được viết)
| Service | Trạng thái |
|---|---|
| `PatientMasterService` | Interface có 6 method nhưng **không có Impl** — gọi vào sẽ lỗi biên dịch/không dùng được |
| `ImportService` | **Không tồn tại** — không có gì điều phối upload → parse → validate → lưu DB |
| `FileParserService` | **Không tồn tại** — mặc dù có `opencsv` và `poi-ooxml` trong `pom.xml`, không có class nào dùng `CSVReader` hoặc `Workbook` |
| `FileValidationService` | **Không tồn tại** — DTO `FileValidationResponse` đã định nghĩa nhưng không ai tạo ra nó |
| `AsyncImportProcessor` | **Không tồn tại** — import file lớn cần xử lý async/background, chưa có `@Async` hay job queue nào |

### 2.3 `PatientMatchingServiceImpl` — logic có nhưng thiếu nhiều phần quan trọng

Đọc trực tiếp file (170 dòng), thấy:

```119:170:be/mpi/demo/src/main/java/com/mpi/demo/service/impl/PatientMatchingServiceImpl.java
    @Override
    public MatchDecisionEnum getAutoDecision(BigDecimal score) {
        if (score.compareTo(new BigDecimal("85.00")) >= 0) {
            return MatchDecisionEnum.AUTO_APPROVED;
        } else if (score.compareTo(new BigDecimal("50.00")) >= 0) {
            return MatchDecisionEnum.PENDING;
        }
        return MatchDecisionEnum.REJECTED;
    }

    @Override
    public void processMatchDecision(Long candidateId, MatchDecisionEnum decision, Long reviewedBy) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
```

Các vấn đề cụ thể:
1. **`processMatchDecision` là stub ném exception** — chức năng "duyệt/từ chối match" hoàn toàn chưa hoạt động.
2. **Thiếu Pass 4 trong blocking strategy** — `PATIENT_IMPORT_ANALYSIS.md` yêu cầu block theo Health Insurance No, hiện chỉ có 3 pass (DOB+tên, SĐT, National ID).
3. **`calculateStringSimilarity` quá thô sơ** — chỉ so khớp chính xác (1.0), substring (0.85), hoặc 0.0. Không dùng Levenshtein distance mặc dù `commons-text` đã có sẵn `LevenshteinDistance`/`JaroWinklerSimilarity`.
4. **Không có "veto rule"** — theo spec, nếu giới tính khác nhau rõ ràng thì phải loại thẳng (score = 0), hiện tại code chỉ cộng điểm khi khớp, không trừ/loại khi sai lệch nghiêm trọng.
5. **Không có weight redistribution** — khi một trường bị thiếu dữ liệu (null), spec yêu cầu phân bổ lại trọng số cho các trường còn lại; code hiện tại bỏ qua đơn giản.
6. **Ngưỡng quyết định không đúng spec** — spec trong `PATIENT_IMPORT_ANALYSIS.md` định nghĩa: `>=92` + có strong identifier → `AUTO_MATCH`, `60-91` → `NEEDS_REVIEW`, `<60` → `NO_MATCH`. Code hiện tại dùng `85/50` và tên enum khác (`AUTO_APPROVED/PENDING/REJECTED`) — không khớp tài liệu thiết kế.

### 2.4 Quan hệ Entity chưa chuẩn (thiết kế dữ liệu)
| Vấn đề | Ảnh hưởng |
|---|---|
| `Patient.masterPatientId` là `Long` thô, không phải `@ManyToOne PatientMaster` | Không tận dụng được JPA cascade/join, dễ sai lệch dữ liệu, phải tự viết query join tay |
| `PatientMaster` không có `@OneToMany<Patient>` ngược lại | Không thể lấy "danh sách bệnh nhân thuộc 1 hồ sơ gốc" bằng JPA thông thường |
| `ImportJob.createdBy` là `Long` thô, không phải `@ManyToOne User` | Tương tự, mất tính toàn vẹn tham chiếu ở tầng ORM |
| `User` entity có nhưng không có `UserRepository`/`UserService`/`UserController` | Không thể tạo/tra cứu user thật, "createdBy"/"reviewedBy" chỉ là số suông |

### 2.5 Bảo mật — chưa có gì (rủi ro cao)
- Không có `SecurityConfig`, không có Spring Security dependency trong `pom.xml`.
- Không có JWT (`JwtTokenProvider`/`JwtUtil`).
- Không có `PasswordEncoder`, mặc dù `User.password` tồn tại — nếu lưu plaintext sẽ là lỗi bảo mật nghiêm trọng.
- Không có `@PreAuthorize` hay bất kỳ kiểm soát phân quyền nào.
- CORS đang mở `allowedOriginPattern("*")` — không phù hợp production.
- **Toàn bộ API hiện tại không yêu cầu đăng nhập, ai cũng gọi được.**

### 2.6 Upload file & xử lý file — chưa có gì thực thi
- `UploadFileRequest` DTO có field `MultipartFile file` nhưng **không controller nào nhận file thật**.
- Chưa cấu hình `spring.servlet.multipart.max-file-size` / `max-request-size` trong `application.yaml`.
- Chưa có nơi lưu file tạm (local disk hoặc object storage) trước khi parse.
- Chưa có class nào dùng `opencsv.CSVReader` hoặc `org.apache.poi.ss.usermodel.Workbook` — dependency có nhưng chết, không dùng.

### 2.7 Database — chưa có migration
- `application.yaml` đặt `ddl-auto: none` → nghĩa là schema phải được tạo bằng tay (SQL script), không tự sync qua Hibernate.
- Không có Flyway/Liquibase.
- Chưa thấy file SQL migration chính thức trong `src/main/resources` (chỉ có tài liệu `db.md` mô tả nhưng chưa apply).
- **Nguy cơ:** entity code và schema DB thật có thể không đồng bộ (đặc biệt các bảng mới `import_job`, `import_job_detail`, `patient_master`, `match_candidate`).

### 2.8 Testing — gần như không có
- Chỉ có 1 file test: `DemoApplicationTests.java` (smoke test mặc định, chỉ load context).
- Không có unit test cho service (đặc biệt matching algorithm — đây là logic quan trọng nhất, cần test kỹ với nhiều case).
- Không có test cho controller (MockMvc) hay repository (`@DataJpaTest`).

## 3. Bảng tổng hợp mức độ hoàn thiện

| Khu vực | % hoàn thiện | Mức độ ưu tiên sửa |
|---|---|---|
| CRUD Facility/SourceSystem/Patient | 100% | — |
| Hạ tầng chung (exception, response wrapper, CORS) | 90% (cần siết CORS) | Thấp |
| Entity/Repository cho Matching & Import | 80% (thiếu quan hệ JPA chuẩn) | Trung bình |
| PatientMatchingService (thuật toán) | 40% (có khung, thiếu veto/redistribution/Levenshtein/decision đúng spec) | **Cao** |
| processMatchDecision (duyệt match) | 0% (stub) | **Cao** |
| PatientMasterService | 0% (chỉ interface) | **Cao** |
| ImportService + FileParserService + FileValidationService | 0% | **Cao nhất** |
| ImportController / MatchReviewController / PatientMasterController | 0% | **Cao nhất** |
| Authentication/Authorization | 0% | **Cao nhất** (bắt buộc trước khi lên production) |
| Database migration | 0% (thủ công) | Trung bình-Cao |
| Unit/Integration test | ~2% | Trung bình |

## 4. Kế hoạch làm tiếp theo chuẩn thực tế (đề xuất thứ tự)

### Giai đoạn A — Vá lỗ hổng thiết kế dữ liệu (nửa ngày)
1. Đổi `Patient.masterPatientId` → `@ManyToOne PatientMaster masterPatient`.
2. Thêm `@OneToMany(mappedBy = "masterPatient") List<Patient> patients` vào `PatientMaster`.
3. Đổi `ImportJob.createdBy` → `@ManyToOne User createdBy`.
4. Viết migration SQL (Flyway) khớp với các thay đổi trên, thêm Flyway dependency vào `pom.xml`, đổi `ddl-auto: none` giữ nguyên nhưng để Flyway quản lý version.

### Giai đoạn B — Hoàn thiện thuật toán Matching (1 ngày)
1. Thêm `LevenshteinDistance` hoặc `JaroWinklerSimilarity` (từ `commons-text`) vào `calculateStringSimilarity`.
2. Thêm Pass 4 (Health Insurance No) vào blocking strategy.
3. Thêm veto rule: giới tính khác rõ ràng → score = 0 ngay, không cộng dồn.
4. Thêm weight redistribution khi field null.
5. Sửa `getAutoDecision` đúng theo `PATIENT_IMPORT_ANALYSIS.md`: `>=92` + strong identifier → `AUTO_MATCH`, `60-91` → `NEEDS_REVIEW`, `<60` → `NO_MATCH` (đổi tên enum `MatchDecisionEnum` nếu cần cho khớp).
6. Implement `processMatchDecision` thật: cập nhật `MatchCandidate.decision`, `reviewedBy`, `reviewedAt`; nếu approved thì gán `Patient.masterPatient`; nếu reject thì giữ patient ở trạng thái pending review khác.
7. Viết `PatientMasterServiceImpl` (create, search, merge 2 master, unlink patient khỏi master).

### Giai đoạn C — Import Pipeline (2-3 ngày)
1. `FileStorageService`: lưu file upload vào thư mục tạm (hoặc S3 sau), sinh `fileToken`.
2. `FileParserService`: dùng `opencsv` cho `.csv`, `poi-ooxml` cho `.xlsx`; trả về danh sách row thô + header.
3. `FileValidationService`: validate từng dòng (required field, format ngày, SĐT, national ID), trả `FileValidationResponse` với `errors`, `preview`.
4. `ImportServiceImpl`: điều phối toàn bộ luồng upload → validate → preview → start import → chạy `PatientMatchingService` cho từng dòng → ghi `ImportJobDetail` → cập nhật `ImportJob` (processedRows, successfulRows...).
5. Dùng `@Async` + `ThreadPoolTaskExecutor` (hoặc Spring Batch nếu file lớn) để không block request khi import file nhiều dòng.
6. `ImportController`: `POST /api/v1/imports/upload`, `POST /api/v1/imports/{id}/validate`, `POST /api/v1/imports/{id}/start`, `GET /api/v1/imports/{id}`, `GET /api/v1/imports/{id}/details`, `POST /api/v1/imports/{id}/cancel`.
7. Cấu hình `spring.servlet.multipart.max-file-size`, `max-request-size` trong `application.yaml`.

### Giai đoạn D — Match Review UI Backend (1 ngày)
1. `MatchReviewController`: `GET /api/v1/match-candidates/pending` (dùng `getPendingReviews` đã có sẵn), `POST /api/v1/match-candidates/{id}/approve`, `POST /api/v1/match-candidates/{id}/reject`.
2. Gắn với `processMatchDecision` đã hoàn thiện ở Giai đoạn B.

### Giai đoạn E — Bảo mật (1-2 ngày, bắt buộc trước khi deploy thật)
1. Thêm `spring-boot-starter-security` + `jjwt` (hoặc `spring-security-oauth2-resource-server` nếu dùng OAuth2/JWT chuẩn).
2. `UserRepository`, `UserServiceImpl` (đăng ký/tra user), mã hoá password bằng `BCryptPasswordEncoder`.
3. `SecurityConfig`: cấu hình filter chain, public endpoint (login) vs protected endpoint.
4. `JwtTokenProvider` + `JwtAuthenticationFilter`.
5. `AuthController`: `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`.
6. Siết `CorsConfig` chỉ cho phép domain frontend thật, bỏ `*`.
7. Thêm `@PreAuthorize` theo role (admin/reviewer/import-operator) cho các endpoint nhạy cảm (approve match, xóa facility...).

### Giai đoạn F — Testing (song song, làm liên tục)
1. Unit test cho `PatientMatchingServiceImpl` — test riêng từng rule (veto, redistribution, Levenshtein threshold) với nhiều case biên.
2. `@DataJpaTest` cho các repository có query blocking phức tạp.
3. `@WebMvcTest` cho controller mới (Import, MatchReview).
4. Integration test luồng import end-to-end với file CSV mẫu nhỏ.

## 5. File/class cần tạo mới (danh sách cụ thể)

```
be/mpi/demo/src/main/java/com/mpi/demo/
├── config/
│   └── SecurityConfig.java                      [MỚI]
├── security/
│   ├── JwtTokenProvider.java                    [MỚI]
│   └── JwtAuthenticationFilter.java              [MỚI]
├── controller/
│   ├── ImportController.java                     [MỚI]
│   ├── MatchReviewController.java                [MỚI]
│   ├── PatientMasterController.java              [MỚI]
│   └── AuthController.java                       [MỚI]
├── service/
│   ├── ImportService.java                        [MỚI]
│   ├── FileParserService.java                    [MỚI]
│   ├── FileValidationService.java                [MỚI]
│   ├── FileStorageService.java                    [MỚI]
│   ├── UserService.java                           [MỚI]
│   └── impl/
│       ├── ImportServiceImpl.java                 [MỚI]
│       ├── FileParserServiceImpl.java             [MỚI]
│       ├── FileValidationServiceImpl.java         [MỚI]
│       ├── FileStorageServiceImpl.java            [MỚI]
│       ├── UserServiceImpl.java                   [MỚI]
│       └── PatientMasterServiceImpl.java          [MỚI - interface đã có]
├── repository/
│   └── UserRepository.java                       [MỚI]
├── dto/request/
│   ├── LoginRequest.java                          [MỚI]
│   └── ApproveMatchRequest.java                   [MỚI]
├── dto/response/
│   ├── LoginResponse.java                         [MỚI]
│   └── MatchCandidateResponse.java                [MỚI]
└── resources/db/migration/
    ├── V1__baseline_schema.sql                    [MỚI - nếu dùng Flyway]
    ├── V2__patient_master_link.sql                [MỚI]
    └── V3__import_tables.sql                      [MỚI]
```

## 6. Kết luận

Backend hiện tại hoàn thiện tốt phần **CRUD nghiệp vụ cơ bản** (Facility, SourceSystem, Patient — ~100%), nhưng **hai nghiệp vụ lõi của hệ thống MPI** — matching bệnh nhân trùng lặp và import dữ liệu hàng loạt — **mới chỉ có khung sườn** (entity + repository), phần logic thực thi và API đều thiếu hoặc là stub. Ngoài ra, **bảo mật hoàn toàn chưa có**, đây là rủi ro nghiêm trọng nhất cần xử lý trước khi đưa vào môi trường thật.

Ưu tiên thực tế: (1) hoàn thiện thuật toán matching + processMatchDecision, (2) xây Import pipeline, (3) thêm bảo mật, (4) migration DB chuẩn, (5) test.
