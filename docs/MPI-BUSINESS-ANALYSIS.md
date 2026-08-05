# Phân Tích Nghiệp Vụ — Hệ Thống MPI (Master Patient Index)

> Tài liệu phân tích nghiệp vụ cho hệ thống **MPI — Master Patient Index**.
> Phục vụ: **Developer + PM** (vừa giải thích domain, vừa map code).
> Scope: **Full — current (CRUD + Search) + đang thiết kế (Merge) + roadmap (Auth, Audit, Soft Delete)**.
> Nguồn tham chiếu: code thật trong `be/mpi/demo/src/main/java/com/mpi/demo/`, `fe/mpi/src/`, `fe/mpi/docs/FE-ANALYSIS.md`, `be/mpi/demo/docs/`.

---

## 1. Bối cảnh & Vấn đề nghiệp vụ

### 1.1 MPI là gì?

**Master Patient Index (MPI)** là hệ thống cốt lõi trong y tế nhằm **quản lý định danh duy nhất** cho mỗi bệnh nhân trong một hệ sinh thái có nhiều nguồn dữ liệu (bệnh viện, phòng khám, hệ thống xét nghiệm, hệ thống bảo hiểm, …).

> **Ví dụ thực tế**: Một bệnh nhân có thể xuất hiện ở nhiều nơi với các thông tin khác nhau:
>
> | Nơi | Họ tên | CCCD | SĐT | Ghi chú |
> |-----|--------|------|-----|---------|
> | Quầy lễ tân BV A | Nguyễn Văn A | 012345678901 | 0912345678 | Đăng ký khám tổng quát |
> | Phòng cấp cứu BV B | Nguyễn Văn A. | 012345678901 | (khác) | Nhập viện cấp cứu |
> | Đăng ký BHYT | NGUYEN VAN A | (khác) | 0912345678 | Cấp thẻ BHYT |
>
> → MPI phải **nhận diện** đây là CÙNG MỘT người, không tạo 3 bản ghi trùng lặp.

### 1.2 Vấn đề MPI phải giải quyết

1. **Duplicate patient records** (trùng hồ sơ) — cùng một người nhưng có 2+ bản ghi.
2. **Identity reconciliation** (đối chiếu định danh) — khi phát hiện trùng, **gộp** (merge) các bản ghi, giữ lại 1 bản ghi "master" (survivor), đánh dấu các bản ghi còn lại là merged.
3. **Audit trail** (truy vết) — phải biết bệnh nhân X hiện tại được gộp từ các bản ghi nào, lý do gì, khi nào.
4. **Lookup nhanh** theo nhiều tiêu chí (tên, CCCD, SĐT, BHYT, giới tính, trạng thái).
5. **Consistency** giữa các hệ thống tích hợp (HIS, LIS, PACS, …) — đảm bảo mọi hệ thống đều tra cứu được cùng một `patient.id`.

### 1.3 Phạm vi hiện tại của dự án

**MVP (đang chạy)**:

- CRUD bệnh nhân (Create, Read, Update, Delete).
- Tìm kiếm động (search theo nhiều field, OR trên keyword fields).
- Phân trang server-side.

**Cốt lõi MPI (đang thiết kế)**:

- **Patient Merge** — gộp 2 bệnh nhân trùng lặp với validation nghiệp vụ.

**Future (planned)**:

- Lịch sử merge (`GET /patients/{id}/merges`).
- Soft delete (thay hard delete).
- Authentication + RBAC (User/Role/Permission).
- Tích hợp HIS/LIS.
- Elasticsearch cho full-text search khi scale > 1M records.

### 1.4 Các bên liên quan (Stakeholders)

| Vai trò | Mối quan tâm |
|---------|--------------|
| **Lễ tân / Tiếp nhận** | Tạo nhanh hồ sơ, tránh trùng, tra cứu theo CCCD/SĐT. |
| **Bác sĩ** | Đọc hồ sơ bệnh nhân, cập nhật trạng thái (ACTIVE → DECEASED). |
| **Admin bệnh viện** | Quản lý nghiệp vụ merge, audit. |
| **IT / DevOps** | Triển khai, monitor, bảo mật. |
| **Hệ thống tích hợp (HIS/LIS)** | Tra cứu `patient.id` theo CCCD. |

---

## 2. Domain Model — Phân tích chi tiết

### 2.1 Entity `Patient` — Trái tim của hệ thống

**File**: `be/mpi/demo/src/main/java/com/mpi/demo/entity/Patient.java`

```java
@Entity
@Table(name = "patients")
public class Patient {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private GenderEnum gender;            // MALE | FEMALE | OTHER | UNKNOWN

    @Column(name = "national_id", length = 20)
    private String nationalId;            // CCCD/CMND — pattern: \d{12}

    @Column(name = "health_insurance_no", length = 20)
    private String healthInsuranceNo;     // Mã BHYT — optional

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;           // SĐT — pattern: ^0\d{9}$

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private PatientStatusEnum status;     // ACTIVE | MERGED | DECEASED | INACTIVE

    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
```

### 2.2 Phân tích từng field (quan điểm nghiệp vụ)

| Field | Nghiệp vụ | Validation | Lý do |
|-------|-----------|------------|-------|
| `id` | Định danh nội bộ, do DB sinh ra | Auto-increment | Không phụ thuộc thông tin bệnh nhân (CCCD có thể null với trẻ sơ sinh). |
| `fullName` | Định danh con người | `@NotBlank`, `@Size(max=255)` | Required vì cần tra cứu & hiển thị. |
| `dateOfBirth` | Hỗ trợ nhận diện trùng | `@Past` (BE) | Có thể NULL với trẻ sơ sinh chưa có CMND, hoặc bệnh nhân không nhớ/giấu. |
| `gender` | Lọc, thống kê | Enum | `UNKNOWN` cho trường hợp không xác định. |
| `nationalId` | **Identifier chính** (HN: identity proof) | `^\d{12}$`, `@NotBlank`, **UNIQUE** | Đây là khóa nghiệp vụ chính. |
| `healthInsuranceNo` | Liên kết BHYT | `@Size(max=20)`, optional | Có thể null (chưa có BHYT). |
| `phoneNumber` | Liên lạc | `^0\d{9}$`, optional | Có thể null (bệnh nhân không có SĐT). |
| `status` | Workflow state | Enum, NOT NULL | Xem §2.4. |
| `createdAt` | Audit | Auto (Hibernate) | Không sửa được. |
| `updatedAt` | Audit | Auto (Hibernate) | Tự cập nhật khi entity thay đổi. |

### 2.3 Tại sao `nationalId` UNIQUE nhưng `phoneNumber` thì KHÔNG?

**Đây là quyết định nghiệp vụ quan trọng:**

- `nationalId` (CCCD) là **identity proof** — mỗi người Việt Nam có 1 CCCD duy nhất. Nếu 2 bản ghi trùng CCCD → gần như chắc chắn cùng người (gọi là **duplicate**).
- `phoneNumber` có thể **dùng chung** (trẻ em dùng SĐT bố mẹ, người già dùng SĐT con) → KHÔNG UNIQUE.
- `healthInsuranceNo` cũng có thể **NULL** hoặc dùng chung theo hộ gia đình.

**Hệ quả**: Index UNIQUE trên `national_id` ở DB sẽ tự động chặn tạo trùng CCCD. Backend check thêm ở service layer (`existsByNationalId`) để trả 409 conflict thay vì 500 SQL error.

### 2.4 `PatientStatusEnum` — State Machine

```java
public enum PatientStatusEnum {
    ACTIVE, MERGED, DECEASED, INACTIVE
}
```

```
[ACTIVE] ────────► [DECEASED]    (bệnh nhân qua đời — workflow tự nhiên)
   │
   ├──► [INACTIVE]               (tạm ngưng điều trị, có thể quay lại ACTIVE)
   │
   └──► [MERGED]                 (bị gộp vào patient khác — TERMINAL state)


[INACTIVE] ──────► [ACTIVE]      (quay lại điều trị)
[DECEASED] ──────► (terminal)     (không có transition ra)
[MERGED] ────────► (terminal)     (không có transition ra — chỉ xem được lịch sử)
```

**Quy tắc nghiệp vụ**:

| Status | Ý nghĩa | Có thể update các field khác? | Có hiển thị trong list mặc định? |
|--------|---------|------------------------------|-----------------------------------|
| `ACTIVE` | Đang điều trị | ✅ Có | ✅ Có |
| `INACTIVE` | Tạm ngưng | ✅ Có | ✅ Có (với filter) |
| `DECEASED` | Đã mất | ✅ Có (chỉ metadata) | ⚠️ Có (cần filter) |
| `MERGED` | Bị gộp vào patient khác | ❌ Không (terminal) | ❌ Ẩn khỏi list mặc định |

### 2.5 `PatientMerge` — Audit trail (planned)

**File**: `be/mpi/demo/docs/DATABASE.md §PatientMerge.java` (đã design, chưa code)

```java
@Entity
@Table(name = "patient_merges")
public class PatientMerge {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survivor_id", nullable = false)
    private Patient survivor;           // Bệnh nhân được giữ lại (master)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merged_id", nullable = false)
    private Patient merged;             // Bệnh nhân bị gộp (set status = MERGED)

    @Column(name = "reason", length = 500)
    private String reason;              // Lý do merge (người dùng gõ tay)

    @Column(name = "merged_at", nullable = false)
    private LocalDateTime mergedAt;     // Auto-fill khi persist

    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

**Quy tắc DB**:

- Cả 2 FK dùng `FetchType.LAZY` (không load survivor/merged khi không cần).
- `ON DELETE RESTRICT` — không cho xóa patient nếu đã từng là survivor/merged.
- Index trên `merged_at` — hỗ trợ retention/archive job.

---

## 3. Use Cases — Hành vi nghiệp vụ

### 3.1 UC01 — Tạo bệnh nhân mới

**Actor**: Lễ tân
**Pre-condition**: User đã mở form "Thêm bệnh nhân mới"
**Luồng chính (Happy path)**:

```
1. User nhập: fullName, dateOfBirth, gender, nationalId, [healthInsuranceNo], [phoneNumber], status
2. Frontend validate (manual):
   - fullName: not blank
   - nationalId: 12 chữ số (regex)
   - phoneNumber: 10 chữ số, bắt đầu bằng 0 (nếu có)
3. FE gọi POST /api/v1/patients (CreatePatientRequest)
4. BE @Valid → validate lại (Jakarta Bean Validation)
   - fullName: @NotBlank, @Size(max=255)
   - nationalId: @NotBlank, @Pattern("\\d{12}")
   - dateOfBirth: @Past
   - phoneNumber: @Pattern("^0\\d{9}$") (optional)
5. PatientService.create(request):
   a. Check existsByNationalId(nationalId) → nếu có → throw DuplicateResourceException → 409
   b. Map request → entity (status default ACTIVE nếu null)
   c. patientRepository.save(entity)
   d. PatientResponse.fromEntity(saved)
6. Controller wrap trong ApiResponse.success("Tạo bệnh nhân thành công", response) → 201
7. FE toast success + refresh table
```

**Code trace** (file → method):

```
fe/mpi/src/features/patients/components/forms/PatientForm.tsx
  └─ handleSubmit(event)
       ├─ payload = { fullName, dateOfBirth, gender, nationalId, healthInsuranceNo, phoneNumber, status }
       │  ⚠️ note, carePlan, visitSlot bị strip (xem §6.2)
       ├─ createPatient(payload)              ← services/patientService.ts
       │    └─ request<ApiResponse<PatientResponse>>('/patients', { method:'POST', body })
       │
       └─ onSaved() → setRefreshKey(k+1)     ← PatientsPage state

be/mpi/demo/src/main/java/com/mpi/demo/controller/PatientController.java
  └─ create(@Valid @RequestBody CreatePatientRequest)
       ├─ @Valid trigger Jakarta Bean Validation
       ├─ patientService.create(request)
       │    └─ PatientServiceImpl.create
       │         ├─ if (existsByNationalId) throw DuplicateResourceException → 409
       │         ├─ new Patient() + map fields
       │         ├─ patientRepository.save(patient)
       │         └─ PatientResponse.fromEntity(saved)
       └─ ResponseEntity.created(location).body(ApiResponse.success(...))
```

**Luồng lỗi**:

| Lỗi | Status | HTTP Code | Message Tiếng Việt | Ai hiển thị |
|-----|--------|-----------|--------------------|-------------|
| CCCD trùng | 409 | CONFLICT | "Bệnh nhân đã tồn tại với CCCD = '012345678901'" | Toast error |
| Validation fail (CCCD không 12 số) | 400 | BAD_REQUEST | `details: ["nationalId: Số CCCD phải gồm 12 chữ số"]` | Toast error |
| dateOfBirth tương lai | 400 | BAD_REQUEST | `details: ["dateOfBirth: Ngày sinh phải là ngày trong quá khứ"]` | Toast error |
| Thiếu fullName | 400 | BAD_REQUEST | `details: ["fullName: Họ tên không được để trống"]` | Toast error |
| Server error | 500 | INTERNAL_SERVER_ERROR | "Đã xảy ra lỗi hệ thống, vui lòng thử lại sau" | Toast error |

### 3.2 UC02 — Tìm kiếm bệnh nhân

**Actor**: Bất kỳ ai truy cập list page
**Đặc thù MPI**: search box = "tên HOẶC CCCD HOẶC SĐT" (cùng 1 chuỗi gửi vào 3 field → backend ghép OR).

**Luồng chính**:

```
1. User gõ vào search box (Topbar) → setSearchText("nguyễn")
2. User nhấn Enter hoặc click icon search → handleSearch()
3. FE build PatientsQuery:
   - fullName = "nguyễn"
   - nationalId = "nguyễn"
   - phoneNumber = "nguyễn"
   - size = DEFAULT_PAGE_SIZE (5)
   - page = 1 (1-based)
4. patientService.getPatients({...query, page, size}):
   - Convert page 1 → 0 (0-based cho Spring Pageable)
   - Build URLSearchParams: fullName, nationalId, phoneNumber, page=0, size=5
   - GET /api/v1/patients?fullName=nguyễn&nationalId=nguyễn&phoneNumber=nguyễn&page=0&size=5
5. BE PatientController.search(search, pageable):
   - Spring bind: search = PatientSearchRequest, pageable = PageRequest(0, 5)
   - PatientService.search(search, pageable)
       ├─ PatientSpecification.build(search)
       │    ├─ if fullName not blank → LIKE %fullName% (lowercase)
       │    ├─ if nationalId not blank → LIKE %nationalId%
       │    ├─ if phoneNumber not blank → LIKE %phoneNumber% (strip spaces)
       │    ├─ keywordPredicates (3 cái trên) → cb.or(...)
       │    ├─ if gender → cb.equal
       │    ├─ if healthInsuranceNo → LIKE %healthInsuranceNo%
       │    ├─ if status → cb.equal
       │    └─ return cb.and(predicates)
       ├─ patientRepository.findBy(spec, q -> q.page(pageable))
       ├─ .map(PatientResponse::fromEntity)
       └─ ResultPagination.fromPage(pageResult)
           ├─ page.getNumber() + 1  ← convert 0-based → 1-based cho FE
           └─ meta = { page, pageSize, pages, total }
6. Response: { statusCode: 200, message: "Lấy danh sách người dùng thành công", data: { meta, result } }
7. FE setPatients(data.content), setMeta(data.meta)
8. Render table + pagination
```

**Code trace**:

```
fe/mpi/src/features/patients/pages/PatientsPage.tsx
  └─ activeSearch (useMemo)
       ├─ if keyword: fullName/nationalId/phoneNumber = keyword (cùng 3 field)
       └─ if appliedFilters.gender/status: ...
  └─ PatientsTable render
       └─ useEffect([refreshKey, search, currentPage, pageSize])
            └─ getPatients({ ...search, page: currentPage, size: pageSize })

fe/mpi/src/services/patientService.ts
  └─ getPatients(query)
       ├─ pageNumber0Based = max(0, (query.page ?? 1) - 1)
       ├─ params.set('fullName', ...) if not blank
       ├─ params.set('nationalId', ...) if not blank
       ├─ params.set('phoneNumber', ...) if not blank
       └─ request(`/patients?${params}`)

be/mpi/demo/src/main/java/com/mpi/demo/specification/PatientSpecification.java
  └─ build(request)
       ├─ keywordPredicates = []
       ├─ if fullName not blank: LIKE %fullName% LOWER
       ├─ if nationalId not blank: LIKE %nationalId% LOWER
       ├─ if phoneNumber not blank: strip spaces + LIKE %phone% LOWER
       ├─ predicateList.add(cb.or(keywordPredicates.toArray))  ← OR
       ├─ if gender: cb.equal
       ├─ if healthInsuranceNo: LIKE %healthInsuranceNo% LOWER
       ├─ if status: cb.equal
       └─ cb.and(predicates.toArray)  ← AND giữa keyword và filter fields
```

**SQL tương đương** (PostgreSQL-like syntax, MySQL sẽ khác tí):

```sql
SELECT * FROM patients
WHERE (
    LOWER(full_name) LIKE '%nguyễn%'
    OR LOWER(national_id) LIKE '%nguyễn%'
    OR LOWER(phone_number) LIKE '%nguyễn%'
)
[AND gender = ?]
[AND LOWER(health_insurance_no) LIKE '%?%']
[AND status = ?]
ORDER BY id ASC
LIMIT 5 OFFSET 5*(page-1)
```

### 3.3 UC03 — Cập nhật bệnh nhân

**Actor**: Lễ tân / Bác sĩ
**Pre-condition**: Patient đang ở trạng thái `ACTIVE` / `INACTIVE` / `DECEASED` (không `MERGED`)

**Luồng chính**:

```
1. User click icon Edit trên row → openUpdateForm(patient)
2. PatientForm mode='update', patient={...}
3. useEffect([patient]) → setValues(...) hydrate form
4. User chỉnh sửa + submit
5. FE build payload = { fullName, dateOfBirth, gender, nationalId, healthInsuranceNo, phoneNumber, status }
6. updatePatient({ ...payload, id: patient.id })
7. BE PUT /api/v1/patients (UpdatePatientRequest với id)
   - @Valid → id phải not null
   - existsByNationalId(newNationalId) && !current.nationalId.equals(newNationalId) → 409
   - Update entity → save
8. FE toast success + refresh table
```

**Đặc biệt — check duplicate CCCD khi update**:

```java
// PatientServiceImpl.update()
if (this.patientRepository.existsByNationalId(request.getNationalId())
        && !patient.getNationalId().equals(request.getNationalId())) {
    throw new DuplicateResourceException("Bệnh nhân", "CCCD", request.getNationalId());
}
```

Logic: nếu user thay đổi CCCD thành 1 CCCD đã tồn tại ở bệnh nhân KHÁC → 409. Nếu giữ nguyên CCCD cũ → OK.

### 3.4 UC04 — Xóa bệnh nhân

**Actor**: Lễ tân / Admin
**Pre-condition**: Patient KHÔNG có trong `patient_merges` (để bảo toàn audit trail)

**Luồng chính**:

```
1. User click icon Delete → confirmDelete modal
2. User click "Xác nhận xoá" → onConfirm()
3. confirmDelete() trong PatientsPage:
   - setIsDeleting(true)
   - try:
       const response = await deletePatient(selectedPatient.id)
       showToast('success', 'Thành công', response.message)
       setRefreshKey(k+1)
   - catch:
       showToast('error', 'Thất bại', error.message)
   - finally:
       setIsDeleting(false)
       setIsDeleteModalOpen(false)
       setSelectedPatient(null)
4. DELETE /api/v1/patients/{id}
5. BE: findById → 404 nếu không có → else delete
```

**Hard delete vs Soft delete**:

- **Hiện tại**: HARD DELETE — `patientRepository.delete(existingPatient)` xóa row khỏi DB.
- **Vấn đề**: nếu patient này từng là survivor/merged trong `patient_merges` → FK `ON DELETE RESTRICT` sẽ throw SQL error → 500.
- **Roadmap**: chuyển sang soft delete (set `status = INACTIVE` thay vì xóa) → vẫn giữ audit trail, không vi phạm FK.

### 3.5 UC05 — Patient Merge (cốt lõi MPI — DESIGN)

**Actor**: Admin bệnh viện (người có quyền cao nhất)
**Pre-condition**: User đã mở Patient Detail, có quyền merge

**Sequence diagram**:

```
User (Admin)              Frontend                Backend (Spring Boot)        Database
   │                         │                          │                          │
   │─ Open patient detail ──►│                          │                          │
   │                         │                          │                          │
   │─ Click "Merge" ────────►│                          │                          │
   │                         │─ Open MergeModal         │                          │
   │                         │  (search + select 2nd)   │                          │
   │                         │                          │                          │
   │─ Search 2nd patient ───►│                          │                          │
   │                         │─ GET /patients?keyword ─►│                          │
   │                         │                          │─ SELECT * FROM patients  │
   │                         │                          │   WHERE full_name LIKE... │
   │                         │                          │◄─ [patient list]         │
   │                         │◄─ [list]                 │                          │
   │                         │                          │                          │
   │─ Select merged patient ►│                          │                          │
   │                         │                          │                          │
   │─ Enter reason ─────────►│                          │                          │
   │                         │                          │                          │
   │─ Click "Confirm" ──────►│                          │                          │
   │                         │─ POST /patients/{id}/merge                         │
   │                         │  body: { mergedId, reason }                        │
   │                         │                          │                          │
   │                         │                          │─ Validate @Valid         │
   │                         │                          │                          │
   │                         │                          │─ BEGIN TRANSACTION       │
   │                         │                          │                          │
   │                         │                          │─ Load survivor (404?)    │
   │                         │                          │   SELECT * FROM patients  │
   │                         │                          │   WHERE id = ? ─────────►│
   │                         │                          │◄─ survivor               │
   │                         │                          │                          │
   │                         │                          │─ Load merged (404?)      │
   │                         │                          │   SELECT * FROM patients  │
   │                         │                          │   WHERE id = ? ─────────►│
   │                         │                          │◄─ merged                 │
   │                         │                          │                          │
   │                         │                          │─ Business Rule 1:        │
   │                         │                          │  survivor != merged      │
   │                         │                          │  → 422 if same           │
   │                         │                          │                          │
   │                         │                          │─ Business Rule 2:        │
   │                         │                          │  merged.status != MERGED │
   │                         │                          │  → 422 if already merged │
   │                         │                          │                          │
   │                         │                          │─ Business Rule 3:        │
   │                         │                          │  survivor.status != MERGED│
   │                         │                          │  → 422 if survivor merged│
   │                         │                          │                          │
   │                         │                          │─ UPDATE merged           │
   │                         │                          │  SET status = 'MERGED' ─►│
   │                         │                          │                          │
   │                         │                          │─ INSERT INTO patient_merges│
   │                         │                          │  (survivor_id, merged_id,│
   │                         │                          │   reason, merged_at) ───►│
   │                         │                          │                          │
   │                         │                          │─ COMMIT TRANSACTION      │
   │                         │                          │                          │
   │                         │                          │─ Build MergePatientResponse│
   │                         │                          │  (survivor + mergedId +  │
   │                         │                          │   mergedAt)              │
   │                         │                          │                          │
   │                         │◄─ 200 ApiResponse ──────│                          │
   │                         │                          │                          │
   │─ Toast success ─────────│                          │                          │
   │─ Redirect to survivor ─►│                          │                          │
```

**Business Rules (đặc thù MPI)**:

| Rule | Kiểm tra | Kết quả nếu fail |
|------|----------|------------------|
| R1 | survivor.id ≠ merged.id | 422 — "Survivor và merged phải là 2 bệnh nhân khác nhau" |
| R2 | survivor.status ≠ MERGED | 422 — "Không thể merge vào bệnh nhân đã bị gộp trước đó" |
| R3 | merged.status ≠ MERGED | 422 — "Bệnh nhân này đã bị gộp trước đó, không thể merge lại" |
| R4 | reason.length ≤ 500 | 400 — validation |
| R5 | Cả 2 phải tồn tại | 404 — nếu 1 trong 2 không tồn tại |

**Critical design decision**: KHÔNG xóa `merged` patient. Chỉ set `status = MERGED` + insert row vào `patient_merges`. Lý do: giữ audit trail, tránh mất dữ liệu y tế.

### 3.6 UC06 — Patient Merge UI (planned)

**Luồng UI** (state machine):

```
┌─────────────────┐
│  PatientList    │
└────────┬────────┘
         │ click "Merge"
         ▼
┌─────────────────┐
│ MergeModal      │
│  ├─ Search box  │ ─── GET /patients?keyword ─── server search
│  ├─ Result list │
│  └─ Selected 2nd│
└────────┬────────┘
         │ select 2nd patient
         ▼
┌─────────────────┐
│MergeConfirmModal│
│  ├─ Survivor: P1│
│  ├─ Merged:  P2 │
│  ├─ Reason: …  │
│  └─ Submit btn  │
└────────┬────────┘
         │ submit
         ▼
┌─────────────────┐
│ Loading overlay │
└────────┬────────┘
         │
         ▼
   ┌─────────┐
   │ Success │ ──→ close modal + toast + refresh list
   └─────────┘

   ┌─────────┐
   │  Error  │ ──→ toast + keep modal open
   └─────────┘
```

**Patterns** (xem `fe/mpi/docs/PROJECT-RULES.md` §19):

- Dùng `useApiToast` cho submit.
- Modal scroll lock + Escape key.
- `onSaved` callback → `setRefreshKey(k+1)`.

---

## 4. Validation Rules — Chi tiết

### 4.1 Backend Validation (Jakarta Bean Validation)

| Field | Annotation | Message |
|-------|-----------|---------|
| `fullName` (Create) | `@NotBlank` | "Họ tên không được để trống" |
| `fullName` (Create) | `@Size(max=255)` | "Họ tên tối đa 255 ký tự" |
| `dateOfBirth` (Create) | `@Past` | "Ngày sinh phải là ngày trong quá khứ" |
| `nationalId` (Create) | `@NotBlank` | "Số CCCD không được để trống" |
| `nationalId` (Create) | `@Pattern("\\d{12}")` | "Số CCCD phải gồm 12 chữ số" |
| `healthInsuranceNo` (Create) | `@Size(max=20)` | "Mã BHYT tối đa 20 ký tự" |
| `phoneNumber` (Create) | `@Pattern("^0\\d{9}$")` | "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng số 0" |
| `id` (Update) | `@NotNull` | "Id không được để trống" |

### 4.2 Frontend Validation (manual)

| Field | Rule | Hiện trạng |
|-------|------|------------|
| `fullName` | not blank | ⚠️ **CHƯA implement** — chỉ required trên HTML, không check JS |
| `nationalId` | 12 chữ số | ⚠️ **CHƯA implement** |
| `phoneNumber` | 10 chữ số, bắt đầu 0 | ⚠️ **CHƯA implement** |
| Backend trả 400 | Parse `details: List<String>` | ⚠️ **CHƯA implement** — hiện chỉ hiện message chung |

**→ Tech debt**: cần migrate sang React Hook Form + Zod (xem `fe/mpi/docs/PROJECT-RULES.md` §8 Phần B).

### 4.3 Business Rules (Service layer)

| Rule | Method | Exception |
|------|--------|-----------|
| CCCD duplicate on create | `existsByNationalId()` | `DuplicateResourceException` → 409 |
| CCCD duplicate on update (different patient) | `existsByNationalId() && !current.equals()` | `DuplicateResourceException` → 409 |
| Patient not found (getById, update, delete) | `findById().orElseThrow()` | `ResourceNotFoundException` → 404 |
| Merge: same id | `survivor.id == merged.id` | `BusinessRuleException` → 422 |
| Merge: already merged | `status == MERGED` | `BusinessRuleException` → 422 |

---

## 5. API Surface — Endpoints

### 5.1 Current endpoints

| Method | Path | Request | Response | Status |
|--------|------|---------|----------|--------|
| GET | `/api/v1/patients` | `PatientSearchRequest` (query) + `Pageable` (query) | `ApiResponse<ResultPagination>` | ✅ |
| GET | `/api/v1/patients/{id}` | - | `ApiResponse<PatientResponse>` | ✅ |
| POST | `/api/v1/patients` | `CreatePatientRequest` (body) | `ApiResponse<PatientResponse>` (201) | ✅ |
| PUT | `/api/v1/patients` | `UpdatePatientRequest` (body) | `ApiResponse<PatientResponse>` (200) | ✅ |
| DELETE | `/api/v1/patients/{id}` | - | `ApiResponse<Void>` (200) | ✅ |

### 5.2 Planned endpoints

| Method | Path | Request | Response | Status |
|--------|------|---------|----------|--------|
| POST | `/api/v1/patients/{survivorId}/merge` | `MergePatientRequest` (body) | `ApiResponse<MergePatientResponse>` (200) | 📋 Design |
| GET | `/api/v1/patients/{id}/merges` | `Pageable` | `ApiResponse<ResultPagination>` | 📋 P3 |
| POST | `/api/v1/auth/login` | `LoginRequest` | `ApiResponse<TokenResponse>` | 📋 P5 — Auth |
| POST | `/api/v1/auth/refresh` | - (cookie) | `ApiResponse<TokenResponse>` | 📋 P5 — Auth |

### 5.3 Response Shape

```json
{
  "statusCode": 200,
  "message": "Lấy danh sách người dùng thành công",
  "data": {
    "meta": { "page": 1, "pageSize": 5, "pages": 3, "total": 12 },
    "result": [
      { "id": 1, "fullName": "Nguyễn Văn A", "dateOfBirth": "1990-05-15", "gender": "MALE", "nationalId": "012345678901", "phoneNumber": "0912345678", "status": "ACTIVE" }
    ]
  }
}
```

**⚠️ Naming bug**: `PatientController.search` trả message "Lấy danh sách **người dùng** thành công" — đây là typo từ code HRM cũ. Cần fix trong `PatientController.search()` line 43.

---

## 6. Inconsistencies & Tech Debt — Phân tích

### 6.1 Frontend ↔ Backend message bug

**File**: `be/mpi/demo/src/main/java/com/mpi/demo/controller/PatientController.java:43`

```java
return ResponseEntity.ok(ApiResponse.success("Lấy danh sách người dùng thành công", result));
//                                                       ^^^^^^^^^^^^^^^^
//                                                       "người dùng" — HRM relic
```

**Fix**: `return ResponseEntity.ok(ApiResponse.success("Lấy danh sách bệnh nhân thành công", result));`

### 6.2 PatientForm có field không có trong backend

**File**: `fe/mpi/src/features/patients/components/forms/PatientForm.tsx`

```tsx
interface PatientFormValues {
    fullName: string
    dateOfBirth: string
    gender: Gender
    nationalId: string
    healthInsuranceNo: string
    phoneNumber: string
    status: PatientStatus
    note: string          // ⚠️ không có trong CreatePatientRequest
    carePlan: string      // ⚠️ không có trong CreatePatientRequest
    visitSlot: string     // ⚠️ không có trong CreatePatientRequest
}
```

Hiện tại `handleSubmit` strip 3 field này trước khi gửi:
```tsx
const payload = {
    fullName: values.fullName,
    dateOfBirth: values.dateOfBirth,
    gender: values.gender,
    nationalId: values.nationalId,
    healthInsuranceNo: values.healthInsuranceNo,
    phoneNumber: values.phoneNumber,
    status: values.status,
    // note, carePlan, visitSlot bị strip
}
```

**Hệ quả**: User nhập ghi chú y tế → bấm Lưu → mất. **Đây là bug UX nghiêm trọng** cho 1 hệ thống y tế.

**Cách giải quyết**:

| Option | Effort | Pros | Cons |
|--------|--------|------|------|
| A. Ẩn 3 field UI (đợi backend support) | 5 phút | Không bug | Mất UX feature |
| B. Lưu tạm `note/carePlan/visitSlot` localStorage | 30 phút | Không mất data | Không persist, không audit |
| C. Bổ sung field vào `Patient` entity + migration | 1-2 ngày | Persistent, audit | Cần change DB, migration |
| D. Tạo `PatientNote` entity riêng (1:N) | 2-3 ngày | Flexible, có thể có nhiều note | Phức tạp hơn |

**Recommended**: Option C (bổ sung field vào Patient) cho MVP, sau đó refactor thành Option D khi cần.

### 6.3 Status `MERGED` có label sai

**File**: `fe/mpi/src/constants/patient.ts:11`

```ts
export const statusLabels: Record<PatientStatus, string> = {
    ACTIVE: 'Đang điều trị',
    MERGED: 'Đã xuất viện',  // ⚠️ "Đã xuất viện" ≠ "Đã gộp"
    DECEASED: 'Đã mất',
    INACTIVE: 'Chờ khám',
}
```

**Issue**: `MERGED` nghĩa là "đã bị gộp vào bệnh nhân khác" (merged), không phải "đã xuất viện". Label này gây hiểu lầm nghiêm trọng.

**Fix**:
```ts
MERGED: 'Đã gộp hồ sơ',
```

### 6.4 DEFAULT_PAGE_SIZE bug

**File**: `fe/mpi/src/services/patientService.ts` (uncommitted fix)

```diff
- export const DEFAULT_PAGE_SIZE = 1
+ export const DEFAULT_PAGE_SIZE = 5
```

Bug cũ: page size = 1 → mỗi trang chỉ có 1 bệnh nhân → UX cực tệ. Đã fix locally.

### 6.5 Không có validation client-side

Backend có `@Valid` nhưng round-trip validation gây UX chậm. Cần migrate sang React Hook Form + Zod (xem `fe/mpi/docs/PROJECT-RULES.md` §8 Phần B).

### 6.6 `summaryText` không update khi pageSize đổi

**File**: `fe/mpi/src/features/patients/components/PatientsTable.tsx:16-21`

```ts
const buildSummary = (meta: PaginationMeta): string => {
    if (meta.total === 0) return 'Chưa có bệnh nhân nào'
    const start = (meta.page - 1) * meta.pageSize + 1
    const end = Math.min(meta.page * meta.pageSize, meta.total)
    return `Hiển thị ${start}-${end} của ${meta.total} bệnh nhân`
}
```

OK — re-derive từ meta mỗi render. ✅

### 6.7 `deletePatient` không check status

Nếu patient đang `MERGED` → backend vẫn cho xóa (FK RESTRICT sẽ throw SQL error → 500). Cần:
- Check status trước (`merged.status == MERGED` → 422).
- Hoặc migrate sang soft delete.

### 6.8 Phone number validation chưa check đầu số nhà mạng

Hiện chỉ check `^0\d{9}$`. Đầu số nhà mạng VN (03x, 05x, 07x, 08x, 09x) có thể check thêm. Tuy nhiên, đầu số thay đổi theo thời gian (vd: 0123 → 0103) → cân nhắc KHÔNG enforce đầu số.

### 6.9 National ID chưa check checksum

Hiện chỉ check 12 chữ số. CCCD Việt Nam có thuật toán checksum (mặc dù không public). Có thể bổ sung nếu cần độ chính xác cao.

### 6.10 Không có cách nào xem merged patient

Nếu 1 bệnh nhân bị merge, status = MERGED. Nhưng cách duy nhất để xem là query DB trực tiếp. Cần:
- GET `/api/v1/patients/{id}/merges` — lấy lịch sử merge.
- Permission riêng (chỉ admin mới được xem).

---

## 7. Sequence Diagrams — Các luồng quan trọng

### 7.1 Patient search (đặc thù OR logic)

```
User        FE                  patientService       PatientController      PatientSpec      JPA/Hibernate      MySQL
 │           │                       │                     │                    │                  │               │
 │─ Type "an"─►│                     │                     │                    │                  │               │
 │           │                       │                     │                    │                  │               │
 │─ Enter ──►│                       │                     │                    │                  │               │
 │           │─ setSearchText("an")  │                     │                    │                  │               │
 │           │─ handleSearch()       │                     │                    │                  │               │
 │           │  setRefreshKey(k+1)   │                     │                    │                  │               │
 │           │                       │                     │                    │                  │               │
 │           │─ useEffect fire ──────►│                     │                    │                  │               │
 │           │                       │─ build query        │                    │                  │               │
 │           │                       │  fullName=an        │                    │                  │               │
 │           │                       │  nationalId=an      │                    │                  │               │
 │           │                       │  phoneNumber=an     │                    │                  │               │
 │           │                       │  page=0 size=5      │                    │                  │               │
 │           │                       │                     │                    │                  │               │
 │           │                       │─ GET /patients?... ─►│                    │                  │               │
 │           │                       │                     │─ search(req, p)    │                  │               │
 │           │                       │                     │─ build(req) ──────►│                  │               │
 │           │                       │                     │                    │─ predicates:    │               │
 │           │                       │                     │                    │  full_name OR    │               │
 │           │                       │                     │                    │  national_id OR  │               │
 │           │                       │                     │                    │  phone_number    │               │
 │           │                       │                     │                    │                  │               │
 │           │                       │                     │                    │─ findBy(spec) ─►│               │
 │           │                       │                     │                    │                  │─ SELECT ... ─►│
 │           │                       │                     │                    │                  │  WHERE ...    │
 │           │                       │                     │                    │                  │  OR / AND     │
 │           │                       │                     │                    │                  │  ORDER BY id  │
 │           │                       │                     │                    │                  │  LIMIT 5      │
 │           │                       │                     │                    │                  │  OFFSET 0     │
 │           │                       │                     │                    │                  │◄─ [rows]     │
 │           │                       │                     │                    │                  │               │
 │           │                       │                     │                    │◄─ List<Patient> ─│               │
 │           │                       │                     │                    │                  │               │
 │           │                       │                     │◄─ Page<PatientResp> │                  │               │
 │           │                       │                     │  (mapped)          │                  │               │
 │           │                       │                     │─ ResultPagination.fromPage()           │               │
 │           │                       │                     │  (page + 1)        │                  │               │
 │           │                       │                     │                    │                  │               │
 │           │                       │◄─ ApiResponse────────│                    │                  │               │
 │           │                       │                     │                    │                  │               │
 │           │◄─ { content, meta } ──│                     │                    │                  │               │
 │           │                       │                     │                    │                  │               │
 │           │─ setPatients(data)    │                     │                    │                  │               │
 │           │─ setMeta(data.meta)   │                     │                    │                  │               │
 │           │                       │                     │                    │                  │               │
 │◄─ render──│                       │                     │                    │                  │               │
 │  table    │                       │                     │                    │                  │               │
```

### 7.2 Patient Create (with duplicate check)

```
User       FE                   request<T>           PatientController        Valid         PatientServiceImpl       MySQL
 │          │                       │                     │                    │                  │                   │
 │─ Submit ►│                       │                     │                    │                  │                   │
 │          │─ createPatient(payload)                    │                    │                  │                   │
 │          │                       │                     │                    │                  │                   │
 │          │─ POST /patients ─────►│                     │                    │                  │                   │
 │          │                       │─ fetch ─────────────────────────────────────►│                  │                   │
 │          │                       │                     │                     │                  │                   │
 │          │                       │                     │─ @Valid annotation ─►│                  │                   │
 │          │                       │                     │                    │                  │                   │
 │          │                       │                     │  fullName: ok      │                  │                   │
 │          │                       │                     │  nationalId: ok    │                  │                   │
 │          │                       │                     │  dateOfBirth: ok   │                  │                   │
 │          │                       │                     │                    │                  │                   │
 │          │                       │                     │─ create(request) ──────────────────────►│                   │
 │          │                       │                     │                    │                  │                   │
 │          │                       │                     │                    │  existsByNationalId() ────────────────►│
 │          │                       │                     │                    │                  │  SELECT COUNT(*) │
 │          │                       │                     │                    │                  │  WHERE national_id│
 │          │                       │                     │                    │                  │◄─ 0              │
 │          │                       │                     │                    │                  │                   │
 │          │                       │                     │                    │                  │  no duplicate     │
 │          │                       │                     │                    │                  │                   │
 │          │                       │                     │                    │                  │─ new Patient()    │
 │          │                       │                     │                    │                  │  map fields       │
 │          │                       │                     │                    │                  │                   │
 │          │                       │                     │                    │                  │─ save() ─────────────────►│
 │          │                       │                     │                    │                  │  INSERT INTO      │
 │          │                       │                     │                    │                  │  patients         │
 │          │                       │                     │                    │                  │◄─ id=42           │
 │          │                       │                     │                    │                  │                   │
 │          │                       │                     │                    │                  │─ PatientResponse.fromEntity()│
 │          │                       │                     │                    │                  │                   │
 │          │                       │                     │◄─ ApiResponse<PatientResponse> ──────────│                   │
 │          │                       │                     │                    │                  │                   │
 │          │                       │                     │─ ResponseEntity.created(location)      │                   │
 │          │                       │                     │  .body(ApiResponse.success("Tạo bệnh nhân thành công", response))                    │
 │          │                       │                     │                    │                  │                   │
 │          │                       │◄─ 201 Created ──────│                    │                  │                   │
 │          │                       │                     │                    │                  │                   │
 │          │◄─ { data: {...} } ────│                     │                    │                  │                   │
 │          │                       │                     │                    │                  │                   │
 │          │─ showToast(success)   │                     │                    │                  │                   │
 │          │─ setRefreshKey(k+1)   │                     │                    │                  │                   │
 │          │                       │                     │                    │                  │                   │
 │◄─ render │                       │                     │                    │                  │                   │
 │  table   │                       │                     │                    │                  │                   │
```

**Duplicate case (CCCD already exists)**:

```
... (same up to existsByNationalId) ...
PatientServiceImpl.create                  MySQL
 │                                          │
 │ existsByNationalId() ───────────────────►│
 │                                          │ SELECT COUNT
 │◄─ 1 (duplicate) ─────────────────────────│
 │                                          │
 │ throw DuplicateResourceException         │
 │   (message: "Bệnh nhân đã tồn tại với CCCD = '012345678901'")│
 │                                          │
GlobalExceptionHandler
 │                                          │
 │ @ExceptionHandler(DuplicateResourceException.class)
 │ ◄─ status 409 CONFLICT
 │ body = ApiResponse.conflict(ex.getMessage())
 │
 ▲
FE ◄─ 409
showToast('error', 'Thất bại', 'Bệnh nhân đã tồn tại với CCCD = ...')
```

### 7.3 Patient Merge (planned)

```
Admin       FE                 PatientController      PatientServiceImpl           MySQL
 │           │                       │                        │                       │
 │─ Submit ─►│                       │                        │                       │
 │  (merge)  │─ POST /patients/{survivorId}/merge            │                       │
 │           │  body: { mergedId, reason }                   │                       │
 │           │                       │                        │                       │
 │           │                       │─ @Valid MergePatientRequest                   │
 │           │                       │                        │                       │
 │           │                       │─ merge(survivorId, req) (@Transactional)        │
 │           │                       │                        │                       │
 │           │                       │                        │─ findById(survivorId) ─►│
 │           │                       │                        │◄─ survivor             │
 │           │                       │                        │                       │
 │           │                       │                        │─ findById(mergedId) ───►│
 │           │                       │                        │◄─ merged               │
 │           │                       │                        │                       │
 │           │                       │                        │─ if survivor.id == merged.id ─throw BusinessRuleException (422)
 │           │                       │                        │                       │
 │           │                       │                        │─ if merged.status == MERGED ─throw BusinessRuleException (422)
 │           │                       │                        │                       │
 │           │                       │                        │─ if survivor.status == MERGED ─throw BusinessRuleException (422)
 │           │                       │                        │                       │
 │           │                       │                        │─ merged.status = MERGED│
 │           │                       │                        │─ merged.save() ───────►│
 │           │                       │                        │  UPDATE patients       │
 │           │                       │                        │  SET status='MERGED'   │
 │           │                       │                        │  WHERE id=mergedId     │
 │           │                       │                        │                       │
 │           │                       │                        │─ new PatientMerge()    │
 │           │                       │                        │  setSurvivor(survivor) │
 │           │                       │                        │  setMerged(merged)     │
 │           │                       │                        │  setReason(reason)     │
 │           │                       │                        │  setMergedAt(now)      │
 │           │                       │                        │─ mergeRepo.save() ────►│
 │           │                       │                        │  INSERT INTO patient_merges
 │           │                       │                        │                       │
 │           │                       │                        │─ COMMIT (end @Transactional)         │
 │           │                       │                        │                       │
 │           │                       │◄─ MergePatientResponse ──                       │
 │           │                       │                        │                       │
 │           │                       │─ ApiResponse.success("Merge bệnh nhân thành công", response)  │
 │           │                       │                        │                       │
 │           │◄─ 200 OK ─────────────│                        │                       │
 │           │                       │                        │                       │
 │           │─ showToast(success)   │                        │                       │
 │           │─ setRefreshKey(k+1)   │                        │                       │
 │           │                       │                        │                       │
 │◄─ Navigate│                       │                        │                       │
 │  to survivor detail              │                        │                       │
```

---

## 8. State Machine — Patient Lifecycle

```
                              ┌──────────────┐
                              │   (start)    │
                              └──────┬───────┘
                                     │ create
                                     ▼
                              ┌──────────────┐
                              │    ACTIVE    │ ◄──────────┐
                              └──────┬───────┘            │
                                     │                    │ (re-activate)
                ┌────────────────────┼────────────────────┤
                │                    │                    │
                │ (tạm ngưng)        │ (qua đời)         │
                ▼                    ▼                    │
         ┌──────────────┐    ┌──────────────┐            │
         │   INACTIVE   │    │   DECEASED   │            │
         └──────────────┘    └──────────────┘            │
                                                         │
         ┌──────────────┐                                │
         │    MERGED    │ (terminal — no transition out)│
         └──────────────┘                                │
              ▲                                           │
              │ merge                                     │
              └───────────────────────────────────────────┘
                          (từ ACTIVE/INACTIVE/DECEASED → MERGED)
```

**Transitions**:

| From | To | Trigger | Audit |
|------|-----|---------|-------|
| (none) | ACTIVE | POST /patients (default) | createdAt |
| ACTIVE | INACTIVE | Manually set status | updatedAt |
| ACTIVE | DECEASED | Manually set status | updatedAt |
| ACTIVE | MERGED | POST /patients/{id}/merge | patient_merges row |
| INACTIVE | ACTIVE | Manually set status | updatedAt |
| INACTIVE | MERGED | POST /patients/{id}/merge | patient_merges row |
| DECEASED | MERGED | POST /patients/{id}/merge (admin) | patient_merges row |
| MERGED | (any) | ❌ Blocked — terminal | - |

---

## 9. Authentication & Authorization — Roadmap

### 9.1 Tại sao chưa có Auth?

**Lý do theo `PROJECT-STATUS.md` (P2)**:

> "Auth chưa được bật — tất cả `/api/v1/patients/**` đang public. Cần thêm User/Role/Permission module trước khi enable JWT."

### 9.2 Planned Roles

Theo `docs/decisions/003-authorization-permission-strategy.md`:

| Role | Permission |
|------|-----------|
| `ADMIN` | Full CRUD + merge + view audit |
| `DOCTOR` | Read all + update patient status (DECEASED) |
| `RECEPTIONIST` | CRUD + read audit (basic) |
| `NURSE` | Read only |

### 9.3 Implementation Plan (P5)

**Backend**:

1. Thêm `User`, `Role`, `Permission` entities.
2. Thêm `users`, `user_roles`, `role_permissions` tables.
3. Spring Security 7 + oauth2-resource-server.
4. JWT HS512 (symmetric key).
5. Access token 15 min + refresh token 7 days.
6. Custom `AuthorizationManager` check permission per endpoint + method.
7. Khi enable: mọi `/api/v1/patients/**` cần Bearer token (trừ `/api/v1/auth/login`).

**Frontend**:

1. `features/auth/` folder: LoginPage, auth store (Zustand).
2. Axios interceptor: attach `Authorization: Bearer <token>` + handle 401 → refresh.
3. `ProtectedRoute` component check auth + role.
4. UI ẩn nút Sửa/Xoá/Merge nếu user không có permission.

### 9.4 Risk & Trade-off

| Risk | Mitigation |
|------|------------|
| Breaking change khi enable auth | Phase riêng, dual-mode (allow public + with-auth) trong transition |
| Token storage | Refresh: HttpOnly cookie (SPA), Body (mobile) — xem `docs/decisions/001` |
| Performance: check role mỗi request | Cache role-permission map in-memory (TTL 5min) |

---

## 10. Scalability — Tại sao kiến trúc hiện tại đáp ứng được

### 10.1 Đáp ứng được < 100K records

Theo `docs/decisions/004-filter-strategy.md §6`:

- JPA Specification: dùng `LIKE %keyword%` ⇒ sẽ **KHÔNG dùng được index** nếu keyword không bắt đầu bằng prefix.
- Tuy nhiên, với < 100K records, full table scan chấp nhận được.
- Index `idx_patients_national_id` (UNIQUE) dùng cho exact match — cực nhanh.

### 10.2 Khi nào cần scale

| Records | Action |
|---------|--------|
| < 100K | JPA Specification (current) |
| 100K - 1M | Bổ sung PostgreSQL full-text search (tsvector) |
| 1M - 10M | Elasticsearch |
| > 10M | Elasticsearch + sharding + read replicas |

### 10.3 Future Scale-out Plan

```
┌─────────────────────────────────────────────────────┐
│             Phase 1 (MVP): < 100K records           │
│  ┌──────────┐      ┌──────────┐      ┌──────────┐   │
│  │ Spring   │ ──── │  MySQL   │      │  SPA     │   │
│  │ Boot     │      │  (mpi)   │      │ (React)  │   │
│  └──────────┘      └──────────┘      └──────────┘   │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│             Phase 2: 100K - 1M records              │
│  ┌──────────┐      ┌──────────┐      ┌──────────┐   │
│  │ Spring   │ ──── │  MySQL   │ ──► │ Redis    │   │
│  │ Boot     │      │  (mpi)   │      │ (cache)  │   │
│  └──────────┘      └──────────┘      └──────────┘   │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│             Phase 3: > 1M records                   │
│  ┌──────────┐      ┌──────────┐      ┌──────────┐   │
│  │ Spring   │ ──── │  MySQL   │      │ Redis    │   │
│  │ Boot     │      │  (main)  │      │ (cache)  │   │
│  └────┬─────┘      └──────────┘      └──────────┘   │
│       │                                              │
│       └────────► ┌──────────┐                        │
│                  │ ES Cluster                       │
│                  │ (search) │                        │
│                  └──────────┘                        │
└─────────────────────────────────────────────────────┘
```

---

## 11. Glossary — Thuật ngữ nghiệp vụ

| Thuật ngữ | Tiếng Việt | Định nghĩa |
|----------|------------|------------|
| **MPI** | Master Patient Index | Hệ thống định danh bệnh nhân thống nhất |
| **Survivor** | Bệnh nhân được giữ lại | Khi merge, bệnh nhân này được giữ làm "master" — giữ lại CCCD, lịch sử |
| **Merged patient** | Bệnh nhân bị gộp | Bệnh nhân bị gộp vào survivor — set status = MERGED |
| **HIS** | Hospital Information System | Hệ thống thông tin bệnh viện |
| **LIS** | Laboratory Information System | Hệ thống thông tin xét nghiệm |
| **PACS** | Picture Archiving and Communication System | Hệ thống lưu trữ hình ảnh y tế |
| **MPI** | Master Patient Index | (đã định nghĩa ở trên) |
| **CCCD** | Căn cước công dân | CMND mới, 12 chữ số |
| **BHYT** | Bảo hiểm y tế | Mã số bảo hiểm y tế |
| **Audit trail** | Truy vết | Lịch sử các thao tác quan trọng (merge, delete, status change) |
| **Soft delete** | Xóa mềm | set status = INACTIVE thay vì DELETE row |
| **Hard delete** | Xóa cứng | DELETE row khỏi DB |
| **Specification** | (JPA) | Pattern build dynamic query với Predicate |
| **Specification** | (nghiệp vụ) | Đặc tả yêu cầu |
| **Idempotent** | Bất biến | API có thể gọi nhiều lần với cùng kết quả |
| **Audit retention** | Lưu giữ audit | Thời gian lưu trữ log/audit |

---

## 12. Roadmap — Lộ trình tổng thể

### Phase 0 — Foundation ✅ DONE
- [x] Exception classes + GlobalExceptionHandler
- [x] ApiResponse wrapper
- [x] ResultPagination
- [x] Enums (Gender, PatientStatus)

### Phase 1 — Patient CRUD ✅ DONE
- [x] Patient entity + repository + DTOs
- [x] PatientService + Spec + Controller
- [x] Frontend types + service + UI

### Phase 2 — Patient Merge (core MPI) 🔄 IN PROGRESS
- [ ] PatientMerge entity + repository
- [ ] MergePatientRequest / MergePatientResponse DTOs
- [ ] PatientService.merge() với @Transactional
- [ ] PatientController POST /{survivorId}/merge
- [ ] GlobalExceptionHandler handle BusinessRuleException
- [ ] Unit tests (success + 4 error cases)
- [ ] Integration test
- [ ] Frontend Merge modal

### Phase 3 — Tests 📋 P3
- [ ] PatientServiceImpl unit tests
- [ ] PatientController integration tests
- [ ] PatientSpecification tests

### Phase 4 — Polish 📋 P3
- [ ] Soft delete (status = INACTIVE)
- [ ] Merge history endpoint `GET /{id}/merges`
- [ ] Search debounce trên frontend
- [ ] Export Excel/PDF
- [ ] Fix PatientForm field note/carePlan/visitSlot
- [ ] Fix `MERGED` label
- [ ] Fix "người dùng" typo trong PatientController
- [ ] Migrate sang React Hook Form + Zod

### Phase 5 — Auth 📋 P5
- [ ] User/Role/Permission module
- [ ] Spring Security + oauth2-resource-server
- [ ] JWT access + refresh
- [ ] Frontend Login + interceptor

### Phase 6 — Integration 📋 P6
- [ ] HIS/LIS integration qua REST/Message Queue
- [ ] FHIR R4 (chuẩn y tế quốc tế) cho Patient resource

### Phase 7 — Scale 📋 P7
- [ ] Elasticsearch khi > 1M records
- [ ] Redis cache
- [ ] Read replicas

---

## 13. Cross-Reference

| Câu hỏi | Tài liệu tham chiếu |
|---------|---------------------|
| Tech stack details | `be/mpi/demo/docs/PROJECT-RULES.md` |
| API endpoints | `be/mpi/demo/docs/API_SPEC.md` |
| Database schema | `be/mpi/demo/docs/DATABASE.md` |
| Architecture | `be/mpi/demo/docs/ARCHITECTURE.md` |
| Filter logic | `be/mpi/demo/docs/decisions/004-filter-strategy.md` |
| Auth strategy (planned) | `be/mpi/demo/docs/decisions/003-authorization-permission-strategy.md` |
| Refresh token (planned) | `be/mpi/demo/docs/decisions/001-refresh-token-strategy.md` |
| File upload (planned) | `be/mpi/demo/docs/decisions/002-file-upload-strategy.md` |
| Current status | `be/mpi/demo/docs/PROJECT-STATUS.md` |
| FE code details | `fe/mpi/docs/FE-ANALYSIS.md` |
| FE rules | `fe/mpi/docs/PROJECT-RULES.md` |
