# Patient Master - Cấu trúc mới hoàn thành ✅

## 📂 Cấu trúc thư mục (giống Patient)

```
features/patient-master/
├── components/
│   └── patient-master-list/           ⭐ Component chính
│       ├── patient-master-list.component.ts
│       ├── patient-master-list.component.html
│       └── patient-master-list.component.css
├── pages/
│   └── patient-master-page/           ⭐ Container đơn giản
│       ├── patient-master-page.component.ts
│       ├── patient-master-page.component.html
│       └── patient-master-page.component.css
├── models/
│   └── patient-master.model.ts        ⭐ Interfaces & types
├── services/
│   └── patient-master.service.ts      ⭐ API service
└── patient-master.routes.ts           ⭐ Routing
```

---

## 🎨 Màu sắc đồng bộ với Patient

### Primary (Blue) - #006194
- Stats card chính (Tổng hồ sơ gốc)
- EMPI badge
- Action button hover (View)

### Secondary (Teal) - #006591
- Stats card phụ (Đang hoạt động)
- Status badge active
- Pulsing dots
- Edit button hover

### Tertiary (Gray) - #545D62
- Stats card bổ sung (Đã gộp)
- Inactive status
- History button hover

### Quaternary (Orange) - #F59E0B
- Stats card ngày (Nạp mới hôm nay)
- Highlight elements

---

## ✅ Các tính năng đã triển khai

### 1. **Stats Cards (4 cards)**
- Tổng hồ sơ gốc (Primary blue)
- Đang hoạt động (Secondary teal + pulse)
- Đã gộp (Tertiary orange)
- Nạp mới hôm nay (Quaternary amber + pulse)

### 2. **Filter Toolbar**
- Search box (theo tên, EMPI, CCCD, BHYT, SĐT)
- Filter by Status (Tất cả/Hoạt động/Ngừng/Đã gộp)
- Filter by Gender (Tất cả/Nam/Nữ/Khác)
- Filter by Age Group (dưới 18/19-35/36-60/trên 60)
- Reset button (rotate animation)
- Export Excel button

### 3. **Data Table**
- Avatar với màu theo giới tính
- EMPI badge nổi bật
- Masked data (CCCD, BHYT, Phone)
- Status badge với pulse animation
- Action buttons (View/Edit/History)
- Hover effects

### 4. **Pagination**
- Tái sử dụng shared component
- Page change handler
- Meta info display

### 5. **Reactive Signals**
- Loading state
- Computed stats (real-time)
- Filter synchronization
- Search integration

---

## 🎯 So sánh với Patient module

| Feature | Patient | Patient Master |
|---------|---------|----------------|
| **Container** | `patients-page` | `patient-master-page` ✅ |
| **List Component** | `patient-list` | `patient-master-list` ✅ |
| **Filter Panel** | Riêng component | Tích hợp inline ✅ |
| **Form Component** | `patient-form` | Chưa có (có thể thêm) |
| **Color Scheme** | Primary/Secondary/Tertiary | Giống hệt ✅ |
| **Stats Cards** | 4 cards | 4 cards ✅ |
| **Avatar Colors** | Gender-based | Gender-based ✅ |
| **Data Masking** | ✅ | ✅ |

---

## 🚀 API Endpoints cần backend

```typescript
GET  /api/patient-masters?page=0&size=10&keyword=...
     → PagedData<PatientMaster>

GET  /api/patient-masters/{id}
     → PatientMaster

PUT  /api/patient-masters/{id}
     → PatientMaster (updated)

DELETE /api/patient-masters/{id}
     → void
```

---

## 📊 Data Model

```typescript
interface PatientMaster {
  id: number;
  enterpriseId: string;        // EMPI
  fullName: string;
  dateOfBirth: string;
  gender: string;              // MALE/FEMALE/OTHER
  nationalId?: string;         // CCCD/CMND
  healthInsuranceNo?: string;  // BHYT
  phoneNumber?: string;
  address?: string;
  status: string;              // ACTIVE/INACTIVE/MERGED
  linkedPatientCount?: number;
  createdAt: string;
  updatedAt: string;
}
```

---

## ✨ UI/UX Features

### Hover Effects
- Stats cards → lift + scale
- Table rows → background highlight
- Action buttons → color + background
- Reset button → rotate animation

### Loading States
- Spinner trong table
- Skeleton loading (có thể thêm)

### Empty States
- Search icon với message
- "Không tìm thấy hồ sơ gốc nào"

### Responsive
- Stats grid → auto-fit minmax(280px)
- Filter toolbar → column on mobile
- Table → horizontal scroll

---

## 🔄 Workflow

```
User Input → Search/Filter
     ↓
PatientMasterService.search()
     ↓
HTTP GET /api/patient-masters
     ↓
Update signals (patientMasters, pageMeta)
     ↓
Computed stats auto-update
     ↓
UI re-render
```

---

## 📝 Next Steps

1. **Backend Integration**
   - Implement API endpoints
   - Test với mock data
   
2. **Detail Page** (tùy chọn)
   - View master detail
   - Linked patients list
   - Merge history
   
3. **Edit Modal** (tùy chọn)
   - Form validation
   - Update master info
   
4. **Export Feature**
   - Excel export với filter
   - PDF report

---

## 🎨 Design Tokens sử dụng

```css
--primary: #006194
--secondary: #006591
--tertiary: #545D62
--surface-container-lowest
--surface-container-low
--on-surface
--on-surface-variant
--outline-variant
```

---

✅ **Hoàn thành: Cấu trúc đồng bộ với Patient module, màu sắc nhất quán!**
