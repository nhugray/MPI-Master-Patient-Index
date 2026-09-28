# 🚀 HƯỚNG DẪN SETUP VÀ CHẠY PATIENT IMPORT

## ✅ Checklist trước khi chạy

### Backend
- [x] Spring Boot application đã có sẵn
- [x] ImportController đã có tất cả endpoints cần thiết
- [x] Database schema đã được tạo (ImportJob, ImportJobDetail tables)
- [x] SourceSystem data đã có trong database

### Frontend  
- [x] Angular 18.2.14 ✓
- [x] RxJS 7.8.2 ✓
- [x] FormsModule available ✓
- [x] HttpClient configured ✓

## 📦 Các file đã được tạo

```
mpi-web/
├── be/mpi/demo/
│   ├── src/main/java/com/mpi/demo/
│   │   ├── controller/ImportController.java ✓ (đã có)
│   │   ├── service/ImportService.java ✓ (đã có)
│   │   ├── entity/ImportJob.java ✓ (đã có)
│   │   └── ... (backend complete)
│   
├── fe/
│   ├── src/app/
│   │   ├── features/patient/
│   │   │   ├── models/
│   │   │   │   └── import.model.ts ✓ (MỚI TẠO)
│   │   │   ├── pages/
│   │   │   │   ├── patient-import-page.component.ts ✓ (MỚI TẠO)
│   │   │   │   ├── patient-import-page.component.html ✓ (MỚI TẠO)
│   │   │   │   └── patient-import-page.component.css ✓ (MỚI TẠO)
│   │   │   └── services/
│   │   │       └── import.service.ts ✓ (MỚI TẠO)
│   │   ├── app.routes.ts ✓ (CẬP NHẬT)
│   │   └── layout/sidebar/sidebar.component.html ✓ (CẬP NHẬT)
│   │
│   └── PATIENT_IMPORT_README.md ✓ (Documentation)
│
└── PATIENT_IMPORT_SUMMARY.md ✓ (Tổng kết)
```

## 🔧 Bước 1: Kiểm tra Backend

### 1.1. Kiểm tra Database
```sql
-- Kiểm tra bảng import_job đã tồn tại
SELECT * FROM import_job LIMIT 1;

-- Kiểm tra bảng import_job_detail đã tồn tại  
SELECT * FROM import_job_detail LIMIT 1;

-- Kiểm tra có source_system nào
SELECT id, name, code FROM source_system WHERE is_active = true;
```

### 1.2. Start Backend
```bash
cd be/mpi/demo
mvn clean install
mvn spring-boot:run
```

### 1.3. Test Backend API
```bash
# Test API có hoạt động không
curl http://localhost:8080/api/v1/source-systems

# Kết quả mong đợi: JSON với danh sách source systems
```

## 🎨 Bước 2: Setup Frontend

### 2.1. Install Dependencies (nếu chưa)
```bash
cd fe
npm install
```

### 2.2. Kiểm tra Environment
**File**: `fe/src/environments/environment.ts`
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api/v1'  // Đảm bảo đúng URL backend
};
```

### 2.3. Start Frontend
```bash
cd fe
ng serve
```

Hoặc với port cụ thể:
```bash
ng serve --port 4200 --open
```

## 🌐 Bước 3: Truy cập và Test

### 3.1. Mở trình duyệt
```
URL: http://localhost:4200/patient-import
```

### 3.2. Test Flow cơ bản

#### Step 1: Upload File
1. Chuẩn bị file CSV test với format:
```csv
fullName,dateOfBirth,gender,phoneNumber,nationalId,localPatientCode
Nguyễn Văn A,1990-01-15,MALE,0912345678,001234567890,BN001
Trần Thị B,1985-05-20,FEMALE,0987654321,009876543210,BN002
```

2. Chọn Source System từ dropdown
3. Drag & drop file vào vùng upload HOẶC click chọn file

#### Step 2: Xem Validation Result
- Kiểm tra số cột, số dòng
- Xem các cột được phát hiện
- Kiểm tra có lỗi không

#### Step 3: Start Import
- Click nút "Bắt đầu Nhập dữ liệu"
- Quan sát job xuất hiện trong "Hàng đợi Đang xử lý"

#### Step 4: Monitor Progress
- Progress bar tự động cập nhật
- Status thay đổi: PROCESSING → COMPLETED
- Xem số dòng đã xử lý

## 🐛 Troubleshooting

### Lỗi: "Cannot connect to backend"
**Giải pháp:**
1. Kiểm tra backend đang chạy: `http://localhost:8080/api/v1/source-systems`
2. Kiểm tra CORS đã được config trong backend:
```java
// Be sure CorsConfig allows localhost:4200
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins("http://localhost:4200")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowCredentials(true);
            }
        };
    }
}
```

### Lỗi: "Source systems list is empty"
**Giải pháp:**
1. Thêm data vào bảng `source_system`:
```sql
INSERT INTO source_system (name, code, facility_id, is_active) 
VALUES ('Epic HIS', 'EPIC', 1, true);

INSERT INTO source_system (name, code, facility_id, is_active) 
VALUES ('Cerner EMR', 'CERNER', 1, true);
```

### Lỗi: "File upload fails"
**Giải pháp:**
1. Kiểm tra file size < 500MB
2. Kiểm tra file type: CSV, XLSX, XLS
3. Kiểm tra backend có accept multipart/form-data
4. Kiểm tra `spring.servlet.multipart.max-file-size` trong application.properties:
```properties
spring.servlet.multipart.max-file-size=500MB
spring.servlet.multipart.max-request-size=500MB
```

### Lỗi: "Progress không cập nhật"
**Giải pháp:**
1. Mở DevTools → Network tab
2. Kiểm tra có request đến `/api/v1/imports?page=0&size=10` mỗi 5 giây không
3. Kiểm tra response trả về status đúng không

### Lỗi: "Component not found"
**Giải pháp:**
```bash
# Clear cache và rebuild
cd fe
rm -rf node_modules/.cache
ng build --configuration development
ng serve
```

## 📱 Test trên Mobile/Tablet

### Chrome DevTools
1. Mở DevTools (F12)
2. Click icon Toggle Device Toolbar (Ctrl+Shift+M)
3. Chọn device: iPhone 12, iPad Air, etc.
4. Test responsive UI

### Real Device
1. Tìm IP của máy: `ipconfig` (Windows) hoặc `ifconfig` (Mac/Linux)
2. Truy cập: `http://[YOUR_IP]:4200/patient-import`
3. Ví dụ: `http://192.168.1.100:4200/patient-import`

## 🎯 Test Cases Checklist

### File Upload
- [ ] Upload file CSV (< 10MB)
- [ ] Upload file XLSX (< 10MB)
- [ ] Upload file lớn (> 100MB)
- [ ] Upload file sai định dạng (PDF, TXT) → Phải reject
- [ ] Upload file > 500MB → Phải reject
- [ ] Drag and drop file
- [ ] Click browse và chọn file

### Validation
- [ ] File hợp lệ → Badge "Hợp lệ" màu xanh
- [ ] File có lỗi → Badge "Có lỗi" màu đỏ
- [ ] Hiển thị đúng số cột, số dòng
- [ ] Hiển thị danh sách cột được phát hiện
- [ ] Hiển thị lỗi validation nếu có

### Import Process
- [ ] Start import thành công
- [ ] Job xuất hiện trong queue
- [ ] Progress bar cập nhật real-time
- [ ] Status chuyển đổi đúng
- [ ] Completed job hiển thị thời gian

### UI/UX
- [ ] Loading spinner khi upload
- [ ] Loading spinner khi load jobs
- [ ] Empty state khi không có jobs
- [ ] Responsive trên mobile (< 768px)
- [ ] Responsive trên tablet (768-1024px)
- [ ] Responsive trên desktop (> 1024px)
- [ ] Hover effects hoạt động
- [ ] Button active states
- [ ] Drag over effect

### Browser Compatibility
- [ ] Chrome (latest)
- [ ] Firefox (latest)
- [ ] Safari (latest)
- [ ] Edge (latest)

## 📊 Sample Test Data

### CSV Format
```csv
fullName,dateOfBirth,gender,phoneNumber,nationalId,localPatientCode,healthInsuranceNo,address
Nguyễn Văn An,1990-05-15,MALE,0912345678,001234567890,BN001,GD4567890123456,Hà Nội
Trần Thị Bình,1985-08-20,FEMALE,0987654321,009876543210,BN002,GD9876543210987,TP.HCM
Lê Văn Cường,1992-12-03,MALE,0901234567,012345678901,BN003,GD1234567890123,Đà Nẵng
Phạm Thị Dung,1988-03-25,FEMALE,0978123456,098765432109,BN004,GD6789012345678,Hải Phòng
Hoàng Văn Em,1995-07-10,MALE,0967890123,087654321098,BN005,GD3456789012345,Cần Thơ
```

### Expected Column Mapping
- `fullName` → fullName ✓
- `dateOfBirth` → dateOfBirth ✓
- `gender` → gender ✓
- `phoneNumber` → phoneNumber ✓
- `nationalId` → nationalId ✓
- `localPatientCode` → localPatientCode ✓
- `healthInsuranceNo` → healthInsuranceNo ✓
- `address` → address ✓

## 🎉 Done!

Nếu tất cả các bước trên đều OK, màn hình import đã sẵn sàng sử dụng!

### Quick Start Commands:
```bash
# Terminal 1 - Backend
cd be/mpi/demo
mvn spring-boot:run

# Terminal 2 - Frontend
cd fe
ng serve --open

# Browser tự động mở: http://localhost:4200/patient-import
```

## 📞 Need Help?

Nếu gặp vấn đề:
1. Kiểm tra console logs (F12 → Console)
2. Kiểm tra network requests (F12 → Network)
3. Kiểm tra backend logs
4. Xem file `PATIENT_IMPORT_README.md` để biết thêm chi tiết

---
**Chúc bạn thành công! 🚀**
