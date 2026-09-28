# Backend MPI - Phân tích thiếu và kế hoạch hoàn thiện

> Ngày rà soát: 23/09/2026  
> Phạm vi: `be/mpi/demo`  
> Mục tiêu: đối chiếu source code thực tế với API/domain MPI, xác định file đã có, file còn thiếu, logic còn thiếu và thứ tự hoàn thiện.

## 1. Kết luận tổng quan

Backend hiện đã có nền tảng CRUD và model cho MPI, nhưng chưa sẵn sàng chạy đầy đủ nghiệp vụ. Ba controller hiện có (`Patient`, `Facility`, `SourceSystem`) mới xử lý CRUD cơ bản. Các luồng quan trọng nhất vẫn thiếu hoặc chỉ là skeleton:

- Chưa có authentication/authorization; toàn bộ API hiện đang public.
- Import CSV/Excel chưa có controller, parser, validation, storage, async processor và implementation service.
- Matching mới tính điểm sơ bộ; chưa lưu candidate, chưa xử lý approve/reject.
- PatientMaster mới có create/link/unlink sơ bộ; chưa có API search/detail/update/merge.
- Database đang dùng `ddl-auto: none` nhưng chưa có Flyway hoặc migration hoàn chỉnh.
- API specification, entity và DTO đang không thống nhất tên bảng, field status và nghiệp vụ merge.
- Test hiện chỉ có context smoke test, chưa bảo vệ nghiệp vụ.

**Đánh giá hiện tại:** nền CRUD có thể tiếp tục sử dụng sau khi sửa contract và validation, nhưng hệ thống chưa thể coi là hoàn thiện hoặc production-ready.

## 2. Quy ước trạng thái

- `DONE`: file/logic cơ bản đã tồn tại và phù hợp, vẫn cần test.
- `PARTIAL`: file đã tồn tại nhưng thiếu nghiệp vụ, sai contract hoặc chưa đủ production behavior.
- `MISSING`: chưa có file hoặc chưa có endpoint cần thiết.
- `BUG`: đã có code nhưng có lỗi chắc chắn hoặc có nguy cơ runtime/data corruption.
- `DECISION`: cần chốt một phương án domain trước khi code.

## 3. Inventory thực tế

### 3.1 Đã có nền tảng

| Khu vực | File chính | Trạng thái | Nhận xét |
|---|---|---:|---|
| Application | `MPIApplication.java` | DONE | Spring Boot entry point. |
| CRUD controller | `FacilityController`, `SourceSystemController`, `PatientController` | PARTIAL | Có CRUD/search, chưa phân quyền, chưa thống nhất URI update và response type. |
| CRUD service | `FacilityServiceImpl`, `SourceSystemServiceImpl`, `PatientServiceImpl` | PARTIAL | Có mapping cơ bản; thiếu transaction, soft-delete/audit và một số duplicate rule. |
| Entity | `Patient`, `PatientMaster`, `Facility`, `SourceSystem`, `User`, `MatchCandidate`, `ImportJob`, `ImportJobDetail` | PARTIAL | Có model chính nhưng schema/docs không khớp, thiếu quan hệ và audit cần thiết. |
| Repository | Các repository tương ứng | PARTIAL | Có query matching/pagination; thiếu query import, auth, uniqueness và merge. |
| DTO | request/response hiện có | PARTIAL | CRUD DTO có; thiếu auth, master, match review, import response an toàn. |
| Exception | `GlobalExceptionHandler`, 2 custom exception | PARTIAL | Thiếu business/authorization/file/import exceptions và xử lý 401/403. |
| Helper | `ApiResponse`, `ResultPagination` | PARTIAL | Cần chuẩn hóa generic type, status code và page metadata. |
| Specification | Facility/SourceSystem/Patient | PARTIAL | Cần đối chiếu OR/AND filter với API spec và field thực tế. |
| Test | `DemoApplicationTests` | PARTIAL | Chỉ kiểm tra context, chưa có test nghiệp vụ. |

### 3.2 File đang được tài liệu cũ ghi là thiếu nhưng thực tế đã tồn tại

Các file sau **không được tạo lại**, cần sửa hoặc hoàn thiện nội dung:

- `service/ImportService.java` đã có interface nhưng `ImportServiceImpl` chưa implements.
- `service/PatientMatchingService.java` và `service/impl/PatientMatchingServiceImpl.java` đã có, nhưng review decision chưa hoạt động.
- `service/PatientMasterService.java` và `service/impl/PatientMasterServiceImpl.java` đã có, nhưng chỉ là flow sơ bộ.
- Các entity/repository import và matching đã có.
- `UploadFileRequest`, `StartImportRequest`, `ImportJobSearchRequest`, `FileValidationResponse`, `PreviewRowDto`, `ValidationErrorDto`, `ImportJobResponse` đã có nhưng cần review contract và serialization.

## 4. Blocker cấp Critical

### C1. Bật authentication và authorization

**Hiện trạng:** `pom.xml` chưa có Spring Security. Không có `SecurityConfig`, JWT filter, token provider, `UserRepository`, auth controller hoặc role model. Các API patient/facility/source-system đều truy cập không cần đăng nhập.

**File cần tạo:**

- `src/main/java/com/mpi/demo/config/SecurityConfig.java`
- `src/main/java/com/mpi/demo/config/JwtConfig.java` hoặc cấu hình tương đương
- `src/main/java/com/mpi/demo/security/JwtTokenProvider.java`
- `src/main/java/com/mpi/demo/security/JwtAuthenticationFilter.java` nếu dùng filter tự viết, hoặc Resource Server decoder nếu dùng OAuth2 Resource Server
- `src/main/java/com/mpi/demo/security/CustomUserDetailsService.java`
- `src/main/java/com/mpi/demo/repository/UserRepository.java`
- `src/main/java/com/mpi/demo/repository/RoleRepository.java` nếu dùng role table
- `src/main/java/com/mpi/demo/entity/Role.java` và `UserRole.java` hoặc field role tối thiểu trong `User`
- `src/main/java/com/mpi/demo/service/AuthService.java`
- `src/main/java/com/mpi/demo/service/impl/AuthServiceImpl.java`
- `src/main/java/com/mpi/demo/controller/AuthController.java`
- `src/main/java/com/mpi/demo/dto/request/LoginRequest.java`
- `src/main/java/com/mpi/demo/dto/request/RegisterUserRequest.java`
- `src/main/java/com/mpi/demo/dto/request/RefreshTokenRequest.java`
- `src/main/java/com/mpi/demo/dto/response/LoginResponse.java`
- `src/main/java/com/mpi/demo/dto/response/UserResponse.java`

**File cần sửa:**

- `pom.xml`: thêm `spring-boot-starter-security`, JWT/OAuth2 dependency và test security.
- `User.java`: thêm trạng thái active/locked, role relation, hoặc thiết kế role table.
- `application.yaml`: lấy secret/issuer/database password từ environment, không commit credential.
- Tất cả controller nghiệp vụ: thêm `@PreAuthorize` theo role sau khi chốt role.
- `GlobalExceptionHandler`: xử lý `AuthenticationException`, `AccessDeniedException` thành 401/403.

**Role tối thiểu đề xuất:**

| Role | Quyền |
|---|---|
| `ADMIN` | Quản lý user, facility, source system, master merge/unlink, cấu hình. |
| `IMPORT_OPERATOR` | Upload, validate, start/cancel/retry import. |
| `REVIEWER` | Xem pending match và approve/reject/create-new. |
| `DATA_STEWARD` | CRUD patient/master và chỉnh sửa dữ liệu nghiệp vụ. |
| `VIEWER` | Chỉ đọc dữ liệu được phép. |

**Logic bắt buộc:**

1. Auth endpoint (`login`, `refresh`, nếu cho phép `register`) là public.
2. Mật khẩu chỉ lưu BCrypt hash, tuyệt đối không trả về trong response/log.
3. Access token ngắn hạn; refresh token phải được rotate/revoke và lưu hash nếu cần logout/audit.
4. JWT phải chứa subject/user id và authorities; filter xác thực chữ ký, issuer/audience và expiration.
5. Method security phải được bật; controller không tự tin vào role từ request body.
6. Không dùng `allowedOriginPattern("*")` cùng `allowCredentials(true)` trong production; cấu hình allowlist từ environment.

## 5. Import pipeline - file và logic cần hoàn thiện

### 5.1 File cần tạo

- `service/FileStorageService.java`
- `service/impl/FileStorageServiceImpl.java`
- `service/FileParserService.java`
- `service/impl/FileParserServiceImpl.java`
- `service/FileValidationService.java`
- `service/impl/FileValidationServiceImpl.java`
- `service/AsyncImportProcessor.java`
- `service/impl/AsyncImportProcessorImpl.java` nếu muốn tách interface
- `config/AsyncConfig.java`
- `controller/ImportController.java`
- `dto/response/ImportJobDetailResponse.java`
- `dto/response/ImportPreviewResponse.java` nếu preview cần metadata/header mapping
- `exception/FileStorageException.java`
- `exception/FileParsingException.java`
- `exception/ImportStateException.java`

### 5.2 File cần sửa

- `service/ImportService.java`: giữ contract rõ ràng, không trả entity trực tiếp.
- `service/impl/ImportServiceImpl.java`: phải `implements ImportService`; hiện tại class rỗng nên hiện trạng không đáp ứng interface.
- `ImportJob`, `ImportJobDetail`: thêm file token/path an toàn, cancellation state, retry metadata, version/configuration nếu cần.
- `ImportRowStatusEnum`: bổ sung thống nhất `PENDING`, `SUCCESS`, `FAILED`, `DUPLICATE`, `WARNING`, `REQUIRES_REVIEW` nếu UI cần các trạng thái này.
- `ImportJobStatusEnum`: cần có trạng thái `CANCELLED` và quy định transition hợp lệ.
- `ImportJobRepository`, `ImportJobDetailRepository`: bổ sung query lọc theo source/status/date và đếm trạng thái.
- `application.yaml`: multipart limit, upload directory, allowed extensions, encoding và async config.
- `pom.xml`: POI/OpenCSV đã có; chỉ thêm dependency nếu parser thực tế cần.

### 5.3 Controller contract đề xuất

`ImportController` tại `/api/v1/imports`:

| Method | Endpoint | Quyền | Service call |
|---|---|---|---|
| `POST` | `/upload` | `IMPORT_OPERATOR` | Nhận `MultipartFile` + source system, lưu file, parse/validate sơ bộ, trả `FileValidationResponse` có `fileToken`. |
| `GET` | `/{jobId}/preview` | `IMPORT_OPERATOR`, `REVIEWER` | Preview row đã parse/validate, có pagination. |
| `POST` | `/start` | `IMPORT_OPERATOR` | Kiểm tra token/source/validation, tạo job, kích hoạt async processor. |
| `GET` | `/{jobId}` | `IMPORT_OPERATOR`, `REVIEWER`, `ADMIN` | Trả status/count/timestamps/error. |
| `GET` | `/{jobId}/details` | `IMPORT_OPERATOR`, `REVIEWER`, `ADMIN` | Trả DTO chi tiết, lọc status + pagination. |
| `GET` | `` | `IMPORT_OPERATOR`, `ADMIN` | Search job theo source/status/date/user. |
| `POST` | `/{jobId}/cancel` | `IMPORT_OPERATOR`, `ADMIN` | Chỉ cancel job đang pending/validating/processing. |
| `POST` | `/{jobId}/retry-failed` | `IMPORT_OPERATOR`, `ADMIN` | Tạo/reprocess các row failed theo policy. |

Controller chỉ nhận request, validate, gọi interface service và bọc `ApiResponse`; không parse file, không cập nhật entity trực tiếp.

### 5.4 Service logic bắt buộc

**Upload/validate:**

1. Kiểm tra file không rỗng, extension/content type, kích thước và tên file.
2. Kiểm tra source system tồn tại và active.
3. Lưu file vào thư mục ngoài classpath bằng UUID/token, không dùng tên file người dùng làm path.
4. Parse CSV/Excel với UTF-8 BOM, header duplicate, empty header, row giới hạn và sheet đầu tiên.
5. Chuẩn hóa header qua mapping cấu hình; báo rõ field bắt buộc bị thiếu.
6. Validate từng row: local code, full name, DOB không ở tương lai, phone, national ID, gender, insurance.
7. Phát hiện duplicate trong cùng file và duplicate với cùng source system.
8. Tạo preview/error theo row number; không tạo patient trước khi user start import.

**Start job:**

1. Kiểm tra file token thuộc user hiện tại, chưa hết hạn và đúng source system.
2. Tạo `ImportJob` trạng thái `PENDING`; lưu total rows và user hiện tại.
3. Chuyển trạng thái `VALIDATING`/`PROCESSING` theo transition, không cho start hai lần.
4. Gọi async processor sau khi transaction tạo job commit.

**Async processor:**

1. Mỗi row chạy trong transaction nhỏ hoặc batch có giới hạn, tránh giữ transaction cho toàn file.
2. Parse row thành `Patient`; tìm duplicate theo `(sourceSystemId, localPatientCode)`.
3. Gọi matching để lấy candidate; nếu không có candidate thì tạo `PatientMaster` mới theo policy.
4. Nếu auto-approved thì link patient vào master; nếu pending thì lưu `MatchCandidate` và đánh dấu row `REQUIRES_REVIEW`.
5. Cập nhật `ImportJobDetail` với row data đã sanitize, score, matched master, warning/error.
6. Cập nhật counters an toàn, có thể resume; không để một row lỗi làm mất toàn bộ job.
7. Nếu cancel được yêu cầu, dừng ở boundary row/batch và set `CANCELLED`.
8. Kết thúc bằng `COMPLETED` nếu không có lỗi hệ thống, `FAILED` nếu pipeline lỗi không thể tiếp tục.
9. Xóa file tạm sau khi job terminal, trừ khi policy retention yêu cầu giữ để audit.

## 6. Matching và match review

### 6.1 Hiện trạng cần sửa

`PatientMatchingServiceImpl` hiện:

- Có các blocking pass theo DOB + 3 ký tự tên, phone last 4 và national ID.
- Tính điểm sơ bộ theo national ID/name/DOB/phone/gender.
- Dùng similarity exact/contains, chưa dùng Levenshtein dù dependency Commons Text đã có.
- Chưa blocking theo health insurance.
- Chưa có gender mismatch veto, missing-field redistribution hoặc strong identifier rule.
- Ngưỡng hiện là `85/50`, cần chốt với business.
- `findMatchCandidates()` chỉ build object, chưa lưu candidate.
- `processMatchDecision()` luôn ném `UnsupportedOperationException`.

### 6.2 File cần tạo

- `controller/MatchReviewController.java`
- `dto/response/MatchCandidateResponse.java`
- `dto/request/MatchDecisionRequest.java` nếu decision/reason gửi trong body
- `dto/response/MatchDecisionResponse.java` nếu cần trả patient/master sau xử lý
- `exception/BusinessRuleException.java`

### 6.3 Repository/entity cần sửa

- `MatchCandidateRepository`: query pending theo source/date/score, kiểm tra candidate trùng patient/master, query lock khi review.
- `MatchCandidate`: thêm decision reason/audit nếu cần; đảm bảo reviewedBy và reviewedAt được set.
- `PatientRepository` và `PatientMasterRepository`: thêm lookup normalized identifiers và active status.
- `PatientMasterService`: expose link/create master qua service, không để controller tự sửa entity.

### 6.4 Controller contract

`MatchReviewController` tại `/api/v1/match-candidates`:

| Method | Endpoint | Quyền | Logic |
|---|---|---|---|
| `GET` | `/pending` | `REVIEWER`, `ADMIN` | Trả candidate DTO, pagination/filter score/source. |
| `GET` | `/{id}` | `REVIEWER`, `ADMIN` | Trả patient, candidate master, score breakdown, decision. |
| `POST` | `/{id}/approve` | `REVIEWER`, `ADMIN` | Gọi service với current authenticated user. |
| `POST` | `/{id}/reject` | `REVIEWER`, `ADMIN` | Bắt buộc reason nếu policy yêu cầu. |
| `POST` | `/{id}/create-new` | `REVIEWER`, `ADMIN` | Tạo master mới và link patient trong một transaction. |

Không nhận `reviewerId` từ client; lấy user id từ `SecurityContext`.

### 6.5 Service logic

1. Candidate phải tồn tại và đang `PENDING`; candidate đã xử lý không được xử lý lại.
2. Approve: kiểm tra patient chưa link master khác hoặc xử lý conflict theo policy; link patient vào candidate master; set patient match status; ghi reviewer/time.
3. Reject: giữ patient chưa link hoặc chuyển trạng thái review tương ứng; ghi reason/reviewer/time.
4. Create-new: tạo master từ patient, link patient, candidate decision thành create-new/rejected theo enum đã chốt.
5. Tất cả thao tác decision phải `@Transactional` và có optimistic locking/idempotency.
6. Lưu `scoreBreakdown` có cấu trúc JSON ổn định để audit và giải thích cho reviewer.

## 7. PatientMaster management

### 7.1 Hiện trạng

`PatientMasterServiceImpl` hiện có create/link/unlink và search nhưng `searchMasters()` bỏ qua keyword. Merge đang bị comment. Chưa có controller hoặc DTO dành riêng cho master. `PatientMaster` cũng chưa có inverse collection patient; nên trả entity trực tiếp dễ gây lazy-loading/JSON recursion.

### 7.2 File cần tạo

- `controller/PatientMasterController.java`
- `dto/request/CreatePatientMasterRequest.java`
- `dto/request/UpdatePatientMasterRequest.java`
- `dto/request/PatientMasterSearchRequest.java`
- `dto/request/MergePatientMasterRequest.java`
- `dto/response/PatientMasterResponse.java`
- `dto/response/PatientMasterDetailResponse.java`
- `dto/response/PatientMasterMergeResponse.java`
- `dto/response/PatientSummaryResponse.java`
- `entity/PatientMasterMerge.java` hoặc entity audit tương đương nếu merge master cần lịch sử.

### 7.3 File cần sửa

- `PatientMasterService.java`: đổi contract từ trả entity sang DTO/service result; thêm getById, createManual, update, merge, search, unlink.
- `PatientMasterServiceImpl.java`: thêm transaction, ResourceNotFound/BusinessRule exception, keyword specification và merge logic.
- `PatientMasterRepository`: query active, keyword, enterprise id và merge state.
- `PatientMaster.java`: cân nhắc `@OneToMany(mappedBy = "masterPatient")` với LAZY; không serialize entity trực tiếp.
- `Patient.java`: quy định rõ match status và master status; thêm optimistic locking nếu cần.

### 7.4 Controller contract

`PatientMasterController` tại `/api/v1/patient-masters`:

| Method | Endpoint | Quyền |
|---|---|---|
| `GET` | `` | `VIEWER`, `DATA_STEWARD`, `ADMIN` |
| `GET` | `/{id}` | `VIEWER`, `DATA_STEWARD`, `ADMIN` |
| `POST` | `` | `DATA_STEWARD`, `ADMIN` |
| `PUT` | `/{id}` | `DATA_STEWARD`, `ADMIN` |
| `POST` | `/{targetId}/merge/{sourceId}` | `ADMIN`, có thể thêm `DATA_STEWARD` theo policy |
| `POST` | `/unlink-patient/{patientId}` | `DATA_STEWARD`, `ADMIN` |

### 7.5 Merge logic

1. Lock target và source master trong transaction.
2. Không cho merge chính nó, master không tồn tại, master đã merged hoặc target/source inactive.
3. Chuyển mọi patient của source sang target.
4. Set source `status = MERGED`, `mergedIntoId = target.id`.
5. Cập nhật candidate pending liên quan nếu cần.
6. Ghi audit record gồm actor, reason, thời gian và snapshot tối thiểu.
7. Không hard-delete source master.
8. Trả DTO target sau merge, không trả JPA entity trực tiếp.

## 8. Sửa CRUD hiện có

### 8.1 Patient

**File:** `PatientServiceImpl`, `PatientController`, request/response DTO, `PatientSpecification`, `PatientRepository`.

Cần làm:

- Quyết định patient có thuộc source system bắt buộc hay không; nếu có thì API spec phải có `sourceSystemId` và `localPatientCode`.
- Kiểm tra duplicate national ID theo business rule: toàn hệ thống hay theo source system. Hiện code chỉ kiểm tra local code trong source.
- Không dùng `PatientResponse` chứa trực tiếp `PatientMaster` entity; tạo `PatientMasterSummaryResponse`.
- Đồng bộ `matchStatus` với API field `status`, hoặc sửa API spec thành `matchStatus`; không để hai contract khác nhau.
- `delete()` cần đổi thành soft delete/inactive hoặc chặn delete khi có master/candidate/import audit.
- Thêm `@Transactional` cho create/update/delete.
- Update phải xử lý national ID conflict và không tự reset match state ngoài policy.
- Đối chiếu filter OR/AND trong `PatientSpecification` với API spec.

### 8.2 Facility và SourceSystem

**File:** `FacilityServiceImpl`, `SourceSystemServiceImpl`, controllers và repositories.

Cần làm:

- Thêm `@Transactional` vào write methods.
- Chặn delete nếu đang được source system/patient/import job tham chiếu; ưu tiên deactivate.
- Chuẩn hóa `PUT /{id}` hoặc giữ body id nhưng API phải thống nhất.
- Dùng `ResponseEntity<ApiResponse<T>>` với URI location tuyệt đối/tương đối đúng (`/api/v1/...`).
- Thêm authorization theo role.
- Bổ sung database unique constraint cho code.

## 9. Entity, DTO và enum cần thống nhất

### 9.1 Vấn đề schema/domain

- `Patient` map vào bảng `patient`, trong khi tài liệu database mô tả `patients`.
- `Patient` có `source_system_id`, `local_patient_code`, `master_patient_id`, `match_status`; tài liệu cũ không mô tả đầy đủ.
- `ImportJob` và `ImportJobDetail` tồn tại trong entity nhưng schema tài liệu cũ chỉ nói về patients/patient_merges.
- `ImportJob.createdBy` là `Long`, trong khi `MatchCandidate.reviewedBy` là relation `User`; cần thống nhất audit model.
- `PatientMaster.mergedIntoId` chỉ là scalar; cần foreign key/self-reference policy.
- `User` tồn tại nhưng không có repository/role/password lifecycle.

### 9.2 DTO cần tạo hoặc chỉnh

- Auth: `LoginRequest`, `RegisterUserRequest`, `RefreshTokenRequest`, `LoginResponse`, `UserResponse`.
- Import: `ImportJobDetailResponse`, file token/preview response, mapping response.
- Matching: `MatchCandidateResponse`, decision request/response.
- Master: create/update/search/detail/merge request và response.
- Shared: pagination generic DTO typed thay vì raw `ResultPagination`.

### 9.3 Enum cần rà soát

- `MatchStatusEnum` và `PatientStatusEnum` đang biểu diễn hai khái niệm; ghi rõ cái nào là trạng thái bản ghi, cái nào là trạng thái matching.
- `ImportRowStatusEnum` phải đủ giá trị được service/controller sử dụng.
- `ImportJobStatusEnum` phải có `CANCELLED` và transition matrix.
- `MatchDecisionEnum` cần có trạng thái cho `CREATE_NEW` hoặc quy định create-new map vào `REJECTED`/decision riêng.

## 10. Database migration và cấu hình

### File cần tạo/sửa

- `src/main/resources/db/migration/V1__baseline_schema.sql`
- Các migration tiếp theo cho auth/roles, merge audit, indexes nếu schema đã có dữ liệu.
- `pom.xml`: Flyway core + database-specific module phù hợp Spring Boot version.
- `application.yaml`: Flyway enabled, locations, datasource từ environment, multipart/file storage.
- `docs/DATABASE.md`: cập nhật theo entity thật, không giữ mô hình chỉ có 2 bảng.

### Bảng tối thiểu cần có

- `facility`
- `source_system`
- `users`
- `roles`, `user_roles` hoặc role strategy đã chốt
- `patient`
- `patient_master`
- `match_candidate`
- `import_job`
- `import_job_detail`
- `patient_master_merge`/audit merge nếu dùng
- refresh token/session table nếu refresh token lưu server-side

### Constraint/index bắt buộc

- Unique `(source_system_id, local_patient_code)`.
- Index `patient.national_id`, `health_insurance_no`, `phone_number`, `date_of_birth`, `match_status`.
- Index candidate theo `(decision, created_at)`.
- Index import detail theo `(import_job_id, status, row_number)`.
- Foreign key không cascade bừa bãi trên dữ liệu audit.
- Enum lưu bằng string, không dùng ordinal.

## 11. Exception và API response

### File cần tạo

- `BusinessRuleException`
- `FileStorageException`
- `FileParsingException`
- `ImportStateException`
- `UnauthorizedException` nếu không dùng exception mặc định

### File cần sửa

- `GlobalExceptionHandler`: thêm 401, 403, 409, 422, file errors, constraint violation, malformed enum/date và giới hạn upload.
- `ApiResponse`: thống nhất `statusCode`, `message`, `data`, `error`, `details`; không trả stack trace hoặc SQL message ra client.
- Validation handler: trả field name + rejected value an toàn, không lộ password/token.

## 12. Test bắt buộc trước khi đánh dấu hoàn thành

### Unit test

- `PatientServiceImplTest`: create/update duplicate, source not found, national ID conflict, delete policy.
- `FacilityServiceImplTest` và `SourceSystemServiceImplTest`: duplicate, reference constraint, update.
- `PatientMatchingServiceImplTest`: exact identifiers, missing fields, gender mismatch, thresholds, normalized Vietnamese names, decision transitions.
- `PatientMasterServiceImplTest`: create/link/unlink/merge/idempotency/invalid state.
- `FileParserServiceTest`: CSV BOM, delimiter, empty header, Excel first sheet, malformed file.
- `FileValidationServiceTest`: required fields, formats, duplicate rows, existing database duplicate.
- `ImportServiceImplTest`: upload/start/cancel/retry/state transition.

### Repository/controller/security test

- Repository query tests với `@DataJpaTest`.
- Controller contract tests với `@WebMvcTest`: status code, validation, pagination, response wrapper.
- Security tests: public auth, 401 thiếu token, 403 sai role, role đúng được phép.
- Integration test: import một file nhỏ từ upload tới patient/master/candidate/job detail.
- Migration test với database test container hoặc MySQL tương thích.

## 13. Thứ tự triển khai đề xuất

### Phase 0 - Chốt contract

- Chốt tên bảng (`patient` hay `patients`), field status, role model, import state machine và threshold matching.
- Cập nhật `API_SPEC.md`, `ARCHITECTURE.md`, `DATABASE.md` theo quyết định cuối.

### Phase 1 - Làm hệ thống chạy an toàn

- Thêm migration baseline.
- Hoàn thiện exception/response.
- Thêm auth, role, JWT/refresh token.
- Khóa các CRUD endpoint bằng `@PreAuthorize`.
- Sửa CORS và secrets.

### Phase 2 - Hoàn thiện Patient/PatientMaster

- Chuẩn hóa DTO và response không lộ entity.
- Sửa duplicate/filter/status.
- Implement master search/detail/update/link/unlink/merge.
- Thêm audit và transaction.

### Phase 3 - Hoàn thiện matching/review

- Chốt scoring, blocking, threshold và explainability.
- Persist candidate, implement decision transaction và review controller.

### Phase 4 - Hoàn thiện import

- Storage, parser, validator, import controller, async processor, cancel/retry.
- Xử lý counters, row status, file retention và idempotency.

### Phase 5 - Test và vận hành

- Unit/controller/security/integration tests.
- Swagger contract, logging/metrics, health check và test migration.
- Chạy `mvnw.cmd test` và `mvnw.cmd verify` trên môi trường có MySQL/test database phù hợp.

## 14. Definition of Done

Chỉ đánh dấu backend hoàn thiện khi tất cả điều kiện sau đạt:

- [ ] API public chỉ còn auth/health/docs theo policy; API nghiệp vụ yêu cầu JWT.
- [ ] Role được kiểm tra ở method/controller và current user được lấy từ security context.
- [ ] CRUD patient/facility/source-system có validation, duplicate rule, transaction và delete policy.
- [ ] PatientMaster có create/search/detail/update/link/unlink/merge và audit.
- [ ] Matching candidate được persist; approve/reject/create-new hoạt động idempotent.
- [ ] Import chạy được CSV và Excel, có preview, validation, async processing, progress, cancel và retry.
- [ ] Mọi controller trả response DTO, không trả JPA entity trực tiếp.
- [ ] Migration tạo đủ bảng/index/foreign key và chạy được từ database rỗng.
- [ ] Không còn `UnsupportedOperationException`, skeleton service hoặc `RuntimeException` nghiệp vụ chung.
- [ ] Không còn credential hard-code và CORS wildcard có credentials.
- [ ] Có test cho các nghiệp vụ critical và build xanh.

## 15. Danh sách file ưu tiên theo mức độ

### P0 - phải tạo/sửa trước

- `pom.xml`
- `application.yaml`
- `config/SecurityConfig.java`
- `security/*`
- `repository/UserRepository.java`
- `service/AuthService.java`, `service/impl/AuthServiceImpl.java`
- `controller/AuthController.java`
- `service/impl/ImportServiceImpl.java`
- `service/FileStorageService.java` + implementation
- `service/FileParserService.java` + implementation
- `service/FileValidationService.java` + implementation
- `service/AsyncImportProcessor.java`
- `controller/ImportController.java`
- `service/impl/PatientMatchingServiceImpl.java`
- `controller/MatchReviewController.java`
- `exception/BusinessRuleException.java`
- migration `V1__baseline_schema.sql`

### P1 - phải hoàn thiện tiếp theo

- `controller/PatientMasterController.java`
- toàn bộ master DTO và `PatientMasterService`
- `MatchCandidateResponse` và decision DTO
- `ImportJobDetailResponse`
- entity/repository merge audit
- CRUD duplicate/status/delete policy
- `GlobalExceptionHandler` và `ApiResponse`

### P2 - chất lượng và vận hành

- unit/controller/security/integration tests
- OpenAPI annotations và API docs
- structured logging, metrics, health check
- retention/cleanup file tạm và audit data

## 16. Kết luận

Không nên chỉ tạo thêm các file theo checklist cũ. Một số file trong checklist đã tồn tại nhưng chưa hoạt động; nếu tạo trùng sẽ làm sai contract và tăng nợ kỹ thuật. Việc cần làm là dùng tài liệu này làm checklist triển khai: trước tiên chốt domain/schema, sau đó bật security và migration, rồi hoàn thiện master/matching/import theo từng transaction boundary và viết test ngay sau mỗi phase.
