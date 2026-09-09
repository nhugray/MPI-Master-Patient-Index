# MPI System - Master Patient Index

Hệ thống quản lý chỉ mục bệnh nhân tập trung (Master Patient Index) giúp đối chiếu và gộp dữ liệu bệnh nhân từ nhiều hệ thống nguồn khác nhau.

## 📋 Mục lục

- [Kiến trúc hệ thống](#kiến-trúc-hệ-thống)
- [Cấu trúc dự án](#cấu-trúc-dự-án)
- [Cơ sở dữ liệu](#cơ-sở-dữ-liệu)
- [Backend API](#backend-api)
- [Frontend](#frontend)
- [Changelog](#changelog)
- [Hướng dẫn cập nhật mới nhất](#hướng-dẫn-cập-nhật-mới-nhất)

---

## 🏗️ Kiến trúc hệ thống

### Stack công nghệ

**Backend:**
- Java Spring Boot
- Spring Data JPA
- MySQL Database
- Maven

**Frontend:**
- Angular 18+
- TypeScript
- Standalone Components
- Signal-based State Management

---

## 📁 Cấu trúc dự án

```
mpi-web/
├── be/mpi/demo/          # Backend (Spring Boot)
│   └── src/main/java/com/mpi/demo/
│       ├── constant/     # Enum constants
│       ├── entity/       # JPA Entities
│       ├── repository/   # Spring Data Repositories
│       ├── service/      # Business Logic
│       ├── dto/          # Data Transfer Objects
│       ├── specification/# JPA Specifications
│       └── controller/   # REST Controllers
│
└── fe/                   # Frontend (Angular)
    └── src/app/
        ├── features/     # Feature modules
        ├── shared/       # Shared components & utilities
        └── layout/       # Layout components
```

---

## 🗄️ Cơ sở dữ liệu

### ERD chính

```
facility (cơ sở y tế)
  └── source_system (hệ thống nguồn)
        └── patient (bệnh nhân cục bộ)
              └── patient_master (bệnh nhân tổng hợp)
```

### Bảng chính

#### `patient` - Bệnh nhân từ hệ thống nguồn
| Cột | Kiểu | Mô tả |
|-----|------|-------|
| id | BIGINT | Primary key |
| source_system_id | BIGINT | FK -> source_system |
| local_patient_code | VARCHAR(100) | Mã BN tại hệ thống nguồn |
| full_name | VARCHAR(255) | Họ tên |
| date_of_birth | DATE | Ngày sinh |
| gender | ENUM | MALE, FEMALE, OTHER |
| national_id | VARCHAR(20) | Số CCCD |
| health_insurance_no | VARCHAR(20) | Số BHYT |
| phone_number | VARCHAR(20) | Số điện thoại |
| address | VARCHAR(500) | Địa chỉ |
| master_patient_id | BIGINT | FK -> patient_master (sau khi đối chiếu) |
| match_status | ENUM | PENDING, MATCHED, NEW_MASTER, REJECTED |

**Ràng buộc UNIQUE:** `(source_system_id, local_patient_code)`

#### `source_system` - Hệ thống nguồn
| Cột | Kiểu | Mô tả |
|-----|------|-------|
| id | BIGINT | Primary key |
| facility_id | BIGINT | FK -> facility |
| code | VARCHAR(50) | Mã hệ thống (unique) |
| name | VARCHAR(255) | Tên hệ thống |
| is_active | BOOLEAN | Trạng thái hoạt động |

#### `patient_master` - Bệnh nhân tổng hợp
Lưu thông tin bệnh nhân sau khi đã đối chiếu và gộp từ nhiều nguồn.

---

## 🔌 Backend API

### Patient API Endpoints

#### 1. Search & List
```http
GET /api/v1/patients?page=0&size=20
```

**Query Parameters:**
- `fullName`: Tìm theo tên
- `nationalId`: Tìm theo CCCD
- `phoneNumber`: Tìm theo SĐT
- `gender`: Lọc theo giới tính (MALE, FEMALE, OTHER)
- `matchStatus`: Lọc theo trạng thái đối chiếu
- `sourceSystemId`: Lọc theo hệ thống nguồn
- `address`: Tìm theo địa chỉ

#### 2. Get by ID
```http
GET /api/v1/patients/{id}
```

#### 3. Create Patient
```http
POST /api/v1/patients
Content-Type: application/json

{
  "sourceSystemId": 1,
  "localPatientCode": "BN001",
  "fullName": "Nguyễn Văn A",
  "dateOfBirth": "1990-01-01",
  "gender": "MALE",
  "nationalId": "123456789012",
  "healthInsuranceNo": "ABC123",
  "phoneNumber": "0123456789",
  "address": "123 Đường ABC, Quận 1, TP.HCM"
}
```

**Validation:**
- `sourceSystemId`: Required, phải tồn tại trong DB
- `localPatientCode`: Required, max 100 ký tự, unique với sourceSystemId
- `fullName`: Required, max 255 ký tự
- `nationalId`: Optional, 12 chữ số
- `phoneNumber`: Optional, 10 chữ số bắt đầu bằng 0

**Mặc định:**
- `matchStatus` = `PENDING`

#### 4. Update Patient
```http
PUT /api/v1/patients/{id}
Content-Type: application/json

{
  "id": 1,
  "sourceSystemId": 1,
  "localPatientCode": "BN001",
  ... (các trường tương tự create)
}
```

#### 5. Delete Patient
```http
DELETE /api/v1/patients/{id}
```

---

## 🎨 Frontend

### Features

#### 1. Patient Management (`/patients`)
- Danh sách bệnh nhân với phân trang
- Tìm kiếm theo tên, CCCD, SĐT
- Lọc theo giới tính, trạng thái đối chiếu
- Tạo/Sửa/Xóa bệnh nhân
- Hiển thị thông tin hệ thống nguồn

#### 2. Facility Management (`/facilities`)
- Quản lý cơ sở y tế
- Phân loại: Bệnh viện, Phòng khám, Khác

### Shared Components

#### Phone Validator (`shared/validators/phone.validator.ts`)
```typescript
// Validate số điện thoại trong form
phoneValidator(): ValidatorFn

// Format số điện thoại để hiển thị
formatPhoneNumber(phone: string): string
// Ví dụ: "0123456789" -> "0123 456 789"
```

### Constants

#### Patient Constants (`features/patient/models/patient.constants.ts`)
```typescript
GENDER_OPTIONS = [
  { value: Gender.MALE, label: 'Nam' },
  { value: Gender.FEMALE, label: 'Nữ' },
  { value: Gender.OTHER, label: 'Khác' }
]

MATCH_STATUS_OPTIONS = [
  { value: MatchStatus.PENDING, label: 'Chờ đối chiếu' },
  { value: MatchStatus.MATCHED, label: 'Đã đối chiếu' },
  { value: MatchStatus.NEW_MASTER, label: 'Master mới' },
  { value: MatchStatus.REJECTED, label: 'Từ chối' }
]
```

---

## 📝 Changelog

### [2026-09-07] - Major Database Schema Update

#### 🔄 Thay đổi cấu trúc Patient

**Backend Changes:**

1. **Entity Patient**
   - ✅ Thêm: `sourceSystem` (ManyToOne relation)
   - ✅ Thêm: `localPatientCode` (mã BN tại hệ thống nguồn)
   - ✅ Thêm: `address` (địa chỉ)
   - ✅ Thêm: `masterPatientId` (FK sau khi đối chiếu)
   - ✅ Thêm: `matchStatus` (PENDING, MATCHED, NEW_MASTER, REJECTED)
   - ❌ Xóa: `status` (PatientStatusEnum cũ)

2. **Enum mới**
   - ✅ Tạo: `MatchStatusEnum`
   - ✅ Sửa: `GenderEnum` (bỏ UNKNOWN)

3. **Repository**
   - ✅ Tạo: `SourceSystemRepository`
   - ✅ Sửa: `PatientRepository` - validation theo `(sourceSystemId, localPatientCode)`

4. **Service Logic**
   - ✅ Create: Mặc định `matchStatus = PENDING`
   - ✅ Create: Validate `sourceSystemId` phải tồn tại
   - ✅ Create: Check unique `(sourceSystemId, localPatientCode)`
   - ✅ Update: Không cho phép sửa `matchStatus` và `masterPatientId` (cần API riêng)

5. **DTO Changes**
   - ✅ `CreatePatientRequest`: Thêm sourceSystemId, localPatientCode, address
   - ✅ `UpdatePatientRequest`: Thêm các trường mới
   - ✅ `PatientResponse`: Thêm sourceSystemId, sourceSystemName, matchStatus, etc.
   - ✅ `PatientSearchRequest`: Thêm matchStatus, sourceSystemId, address

**Frontend Changes:**

1. **Shared Utilities**
   - ✅ Tạo: `phone.validator.ts` - Validate & format số điện thoại chung
   - ✅ Dùng chung trong: patient-form, facility-form, patient-list, facility-list

2. **Patient Constants**
   - ✅ Tạo: `patient.constants.ts` - GENDER_OPTIONS, STATUS_OPTIONS
   - ✅ Dùng chung trong: patient-filter-panel, patient-list

3. **Code Refactoring**
   - ✅ Loại bỏ code trùng lặp về format phone
   - ✅ Centralize constants thay vì define trong từng component

---

## 🚀 Hướng dẫn sử dụng

### Backend

1. Cấu hình database trong `application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/mpi_system
spring.datasource.username=root
spring.datasource.password=yourpassword
```

2. Chạy SQL script trong `be/mpi/demo/src/main/java/com/mpi/demo/db.md`

3. Start backend:
```bash
cd be/mpi/demo
mvn spring-boot:run
```

### Frontend

1. Install dependencies:
```bash
cd fe
npm install
```

2. Start dev server:
```bash
npm start
```

3. Mở browser: `http://localhost:4200`

---

## 🆕 Hướng dẫn cập nhật mới nhất (Sep 7, 2026)

### ✅ Đã hoàn thành

#### Backend:
1. **Enum mới:**
   - `MatchStatusEnum` - Trạng thái đối chiếu (PENDING, MATCHED, NEW_MASTER, REJECTED)
   - `GenderEnum` - Bỏ UNKNOWN (chỉ còn MALE, FEMALE, OTHER)

2. **Entity mới/cập nhật:**
   - `SourceSystem` - Entity cho hệ thống nguồn
   - `Patient` - Thêm: sourceSystem, localPatientCode, address, masterPatientId, matchStatus
   - Bỏ: status cũ (PatientStatus)

3. **DTOs cập nhật:**
   - `CreatePatientRequest` - Thêm sourceSystemId, localPatientCode, address (bắt buộc)
   - `UpdatePatientRequest` - Tương tự
   - `PatientResponse` - Thêm đầy đủ thông tin mới
   - `PatientSearchRequest` - Thêm matchStatus, sourceSystemId, localPatientCode, address

4. **Repository & Service:**
   - `SourceSystemRepository` - Repository mới
   - `PatientRepository` - Sửa validation unique theo (sourceSystemId + localPatientCode)
   - `PatientServiceImpl` - Logic CRUD mới, tự động set matchStatus = PENDING khi tạo

5. **Specification:**
   - `PatientSpecification` - Thêm filter theo matchStatus, sourceSystemId, address

#### Frontend:
1. **Enum mới:**
   - `MatchStatus` - Trạng thái đối chiếu (PENDING, MATCHED, NEW_MASTER, REJECTED)

2. **Models cập nhật:**
   - `Patient` interface - Thêm sourceSystemId, sourceSystemName, localPatientCode, address, matchStatus, masterPatientId
   - `PatientSearchParams` - Thêm matchStatus, sourceSystemId, localPatientCode, address
   - `patient.constants.ts` - Thêm MATCH_STATUS_OPTIONS, bỏ Gender.UNKNOWN

3. **Components cập nhật:**
   - `patient-form` - Thêm fields: sourceSystemId, localPatientCode, address
   - `patient-list` - Hiển thị: sourceSystemName, localPatientCode, matchStatus
   - `patient-filter-panel` - Filter theo matchStatus thay vì status cũ

### 🔑 Logic nghiệp vụ mới

**Tạo bệnh nhân:**
- Bắt buộc: sourceSystemId, localPatientCode, fullName
- Tự động: matchStatus = PENDING
- CCCD không bắt buộc (có thể null)

**Validation unique:**
- Không còn validate theo CCCD
- Validate theo: (sourceSystemId + localPatientCode)

**Matching workflow:**
- matchStatus & masterPatientId chỉ được update bởi matching engine (API riêng)
- CRUD API thông thường không cho phép thay đổi 2 trường này

---

## 🔮 Next Steps

- [ ] Implement Matching Algorithm (đối chiếu bệnh nhân)
- [ ] API riêng để update matchStatus và masterPatientId
- [ ] Match Candidate management
- [ ] Patient Master CRUD
- [ ] Source System CRUD
- [ ] Facility CRUD
- [ ] Dashboard & Analytics
- [ ] User Authentication & Authorization

---

## 📞 Liên hệ

Nếu có vấn đề hoặc câu hỏi, vui lòng liên hệ team phát triển.
