/**
 * PatientFilterPanel — bộ lọc bệnh nhân.
 *
 * Chỉ hỗ trợ lọc theo field enum (Giới tính + Trạng thái) vì đó là yêu cầu
 * nghiệp vụ hiện tại và khớp với các giá trị mà PatientController.search
 * chấp nhận (PatientSearchRequest.gender / status — GenderEnum / PatientStatusEnum
 * ở backend). Mọi giá trị chọn được ghép vào query string bởi services/patientService.
 *
 * CSS đặt riêng (BEM namespace `patient-filter-*`) để filter panel không
 * đụng với CSS của các modal/table, dễ bảo trì.
 */
import type { Gender, PatientStatus, PatientSearchRequest } from '../../../../types/patient'
import { genderLabels, statusLabels } from '../../../../constants/patient'
import './PatientFilterPanel.css'

export type PatientFilterValues = Pick<PatientSearchRequest, 'gender' | 'status'>

interface PatientFilterPanelProps {
    values: PatientFilterValues
    onChange: (field: keyof PatientFilterValues, value: string) => void
    onApply: () => void
    onReset: () => void
    resultCount?: number | null
    isLoading?: boolean
}

const GENDER_OPTIONS: Gender[] = ['MALE', 'FEMALE', 'OTHER', 'UNKNOWN']
const STATUS_OPTIONS: PatientStatus[] = ['ACTIVE', 'MERGED', 'DECEASED', 'INACTIVE']

export function PatientFilterPanel({
    values,
    onChange,
    onApply,
    onReset,
    resultCount,
    isLoading,
}: PatientFilterPanelProps) {
    const hasActiveFilter = Boolean(values.gender) || Boolean(values.status)

    return (
        <section className="patient-filter" aria-label="Bộ lọc bệnh nhân">
            <header className="patient-filter-header">
                <div className="patient-filter-title">
                    <span className="material-symbols-outlined" aria-hidden="true">
                        filter_list
                    </span>
                    <div>
                        <h3>Bộ lọc bệnh nhân</h3>
                        <p>Lọc nhanh theo tiêu chí hệ thống.</p>
                    </div>
                </div>

                {typeof resultCount === 'number' ? (
                    <span className="patient-filter-count">
                        {isLoading ? 'Đang tải...' : `${resultCount} kết quả`}
                    </span>
                ) : null}
            </header>

            <div className="patient-filter-body">
                <div className="patient-filter-field">
                    <label htmlFor="filter-gender" className="patient-filter-label">
                        <span className="material-symbols-outlined" aria-hidden="true">
                            wc
                        </span>
                        Giới tính
                    </label>
                    <div className="patient-filter-select-wrap">
                        <select
                            id="filter-gender"
                            className="patient-filter-select"
                            value={values.gender ?? ''}
                            onChange={(event) => onChange('gender', event.target.value)}
                        >
                            <option value="">Tất cả</option>
                            {GENDER_OPTIONS.map((value) => (
                                <option key={value} value={value}>
                                    {genderLabels[value]}
                                </option>
                            ))}
                        </select>
                        <span className="material-symbols-outlined patient-filter-caret" aria-hidden="true">
                            expand_more
                        </span>
                    </div>
                </div>

                <div className="patient-filter-field">
                    <label htmlFor="filter-status" className="patient-filter-label">
                        <span className="material-symbols-outlined" aria-hidden="true">
                            monitor_heart
                        </span>
                        Trạng thái
                    </label>
                    <div className="patient-filter-select-wrap">
                        <select
                            id="filter-status"
                            className="patient-filter-select"
                            value={values.status ?? ''}
                            onChange={(event) => onChange('status', event.target.value)}
                        >
                            <option value="">Tất cả</option>
                            {STATUS_OPTIONS.map((value) => (
                                <option key={value} value={value}>
                                    {statusLabels[value]}
                                </option>
                            ))}
                        </select>
                        <span className="material-symbols-outlined patient-filter-caret" aria-hidden="true">
                            expand_more
                        </span>
                    </div>
                </div>
            </div>

            <footer className="patient-filter-actions">
                <button
                    type="button"
                    className="patient-filter-button ghost"
                    onClick={onReset}
                    disabled={!hasActiveFilter}
                >
                    <span className="material-symbols-outlined" aria-hidden="true">
                        refresh
                    </span>
                    Đặt lại
                </button>
                <button
                    type="button"
                    className="patient-filter-button primary"
                    onClick={onApply}
                    disabled={isLoading}
                >
                    <span className="material-symbols-outlined" aria-hidden="true">
                        check
                    </span>
                    {isLoading ? 'Đang áp dụng...' : 'Áp dụng'}
                </button>
            </footer>
        </section>
    )
}
