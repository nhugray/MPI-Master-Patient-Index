# Project: MPI-WEB — Master Patient Index

Hệ thống **Master Patient Index (MPI)** — quản lý hồ sơ bệnh nhân thống nhất, hỗ trợ
gộp hồ sơ trùng lặp (merge). Backend Spring Boot 4 + JPA + MySQL, frontend React + TypeScript.

## Đọc trước khi code
- **Coding conventions**: `docs/PROJECT-RULES.md` — đọc file này ĐẦU TIÊN, tuân thủ nghiêm ngặt
- **Tiến độ hiện tại**: `docs/PROJECT-STATUS.md` — biết đang ở đâu trước khi bắt đầu
- **Kiến trúc**: `docs/ARCHITECTURE.md`
- **API endpoints**: `docs/API_SPEC.md`
- **Database schema**: `docs/DATABASE.md`
- **Lý do quyết định**: `docs/decisions/`
- **Context từng module**: mỗi feature có `CONTEXT.md` — đọc trước khi sửa module đó

## Domain ngắn gọn
- **Patient** — hồ sơ bệnh nhân thống nhất (CMND/CCCD, BHYT, SĐT, ngày sinh, giới tính)
- **Patient Merge** — gộp 2 hồ sơ trùng lặp thành 1 (status `MERGED` cho bản ghi bị gộp)
- **Filter / Search** — tra cứu nhanh theo tên, CCCD, SĐT, BHYT, giới tính, trạng thái

## Scope hiện tại (MVP)
- CRUD Patient (tạo, xem, sửa, xóa) với validation chuẩn VN (CMND 12 số, SĐT 10 số bắt đầu bằng 0)
- Filter/search có phân trang (JPA Specification + Pageable)
- Merge 2 bệnh nhân: chọn survivor + merged target, audit timestamp
- Trả response thống nhất qua `ApiResponse<T>`
- Xử lý exception tập trung qua `GlobalExceptionHandler`
