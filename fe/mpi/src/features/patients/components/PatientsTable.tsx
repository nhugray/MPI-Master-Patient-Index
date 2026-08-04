import { useEffect, useState } from 'react'
import { genderLabels, statusLabels, statusTone } from '../../../constants/patient'
import { DEFAULT_PAGE_SIZE, PAGE_SIZE_OPTIONS, getPatients } from '../../../services/patientService'
import type { PaginationMeta, PatientResponse, PatientSearchRequest } from '../../../types/patient'
import { Pagination } from '../../../components/pagination/Pagination'

interface PatientsTableProps {
    refreshKey: number
    search?: PatientSearchRequest
    isFilterActive: boolean
    onToggleFilter: () => void
    onEdit: (patient: PatientResponse) => void
    onDelete: (patient: PatientResponse) => void
}

const buildSummary = (meta: PaginationMeta): string => {
    if (meta.total === 0) return 'Chưa có bệnh nhân nào'
    const start = (meta.page - 1) * meta.pageSize + 1
    const end = Math.min(meta.page * meta.pageSize, meta.total)
    return `Hiển thị ${start}-${end} của ${meta.total} bệnh nhân`
}

const summarizeSearch = (search?: PatientSearchRequest): string =>
    JSON.stringify({
        fullName: search?.fullName ?? '',
        gender: search?.gender ?? '',
        nationalId: search?.nationalId ?? '',
        phoneNumber: search?.phoneNumber ?? '',
        status: search?.status ?? '',
    })

export function PatientsTable({
    refreshKey,
    search,
    isFilterActive,
    onToggleFilter,
    onEdit,
    onDelete,
}: PatientsTableProps) {
    const [patients, setPatients] = useState<PatientResponse[]>([])
    const [meta, setMeta] = useState<PaginationMeta | null>(null)
    const [isLoading, setIsLoading] = useState(false)
    const [currentPage, setCurrentPage] = useState(1)
    const [pageSize, setPageSize] = useState<number>(DEFAULT_PAGE_SIZE)

    // Track search signature + pageSize để reset currentPage về 1 khi user
    // thay đổi điều kiện lọc hoặc số bệnh nhân/trang. Dùng pattern "set state
    // during render" để tránh cascade-renders (eslint react-hooks/set-state-in-effect).
    const [trackedKey, setTrackedKey] = useState<string>(`${refreshKey}|${pageSize}|${summarizeSearch(search)}`)
    const liveKey = `${refreshKey}|${pageSize}|${summarizeSearch(search)}`
    if (trackedKey !== liveKey) {
        setTrackedKey(liveKey)
        setCurrentPage(1)
    }

    useEffect(() => {
        let cancelled = false

        const run = async () => {
            setIsLoading(true)
            try {
                const data = await getPatients({ ...search, page: currentPage, size: pageSize })
                if (cancelled) {
                    setIsLoading(false)
                    return
                }
                setPatients(data.content)
                setMeta(data.meta)
            } catch {
                if (!cancelled) {
                    setPatients([])
                    setMeta(null)
                }
            } finally {
                if (!cancelled) setIsLoading(false)
            }
        }

        void run()
        return () => {
            cancelled = true
        }
    }, [refreshKey, search, currentPage, pageSize])

    const totalPages = meta?.pages ?? 0
    const summaryText = meta ? buildSummary(meta) : 'Đang tải...'

    return (
        <section className="table-card">
            <div className="table-toolbar">
                <div className="toolbar-left">
                    <h3>Tất cả bệnh nhân</h3>
                </div>

                <div className="toolbar-right">
                    <button
                        type="button"
                        className={`ghost-button${isFilterActive ? ' is-active' : ''}`}
                        aria-pressed={isFilterActive}
                        aria-label="Mở bộ lọc bệnh nhân"
                        onClick={onToggleFilter}
                    >
                        <span className="material-symbols-outlined">filter_list</span>
                        Bộ lọc
                        {isFilterActive ? <span className="toolbar-dot" aria-hidden="true" /> : null}
                    </button>
                    <button type="button" className="ghost-button">
                        <span className="material-symbols-outlined">download</span>
                        Xuất Excel
                    </button>
                </div>
            </div>

            <div className="table-scroll">
                <table>
                    <thead>
                        <tr>
                            <th>Họ và tên</th>
                            <th>Ngày sinh</th>
                            <th>Giới tính</th>
                            <th>CCCD / CMDND</th>
                            <th>Số điện thoại</th>
                            <th>Trạng thái</th>
                            <th>Thao tác</th>
                        </tr>
                    </thead>
                    <tbody>
                        {isLoading && patients.length === 0 ? (
                            <tr>
                                <td colSpan={7} className="table-empty">
                                    Đang tải dữ liệu...
                                </td>
                            </tr>
                        ) : patients.length === 0 ? (
                            <tr>
                                <td colSpan={7} className="table-empty">
                                    Không có bệnh nhân nào khớp với bộ lọc hiện tại.
                                </td>
                            </tr>
                        ) : (
                            patients.map((patient) => (
                                <tr key={patient.id}>
                                    <td>
                                        <div className="patient-cell">
                                            <span className="patient-avatar">{patient.fullName.slice(0, 2).toUpperCase()}</span>
                                            <div>
                                                <strong>{patient.fullName}</strong>
                                                <small>{patient.nationalId}</small>
                                            </div>
                                        </div>
                                    </td>
                                    <td>{patient.dateOfBirth}</td>
                                    <td>{genderLabels[patient.gender]}</td>
                                    <td>{patient.nationalId}</td>
                                    <td>{patient.phoneNumber ?? '—'}</td>
                                    <td>
                                        <span className={`status-badge ${statusTone[patient.status]}`}>
                                            {statusLabels[patient.status]}
                                        </span>
                                    </td>
                                    <td>
                                        <div className="action-cell">
                                            <button
                                                type="button"
                                                className="action-button"
                                                aria-label="Chỉnh sửa"
                                                onClick={() => onEdit(patient)}
                                            >
                                                <span className="material-symbols-outlined">edit</span>
                                            </button>
                                            <button
                                                type="button"
                                                className="action-button"
                                                aria-label="Xóa"
                                                onClick={() => onDelete(patient)}
                                            >
                                                <span className="material-symbols-outlined">delete</span>
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>

            <div className="table-footer">
                <span className="table-footer-summary">{summaryText}</span>
                {meta && meta.total > 0 ? (
                    <Pagination
                        currentPage={currentPage}
                        totalPages={totalPages}
                        pageSize={pageSize}
                        pageSizeOptions={PAGE_SIZE_OPTIONS}
                        onPageChange={(page) => {
                            setCurrentPage(page)
                        }}
                        onPageSizeChange={(size) => {
                            setPageSize(size)
                        }}
                        isLoading={isLoading}
                    />
                ) : null}
            </div>
        </section>
    )
}
