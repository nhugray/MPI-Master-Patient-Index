import { useMemo, useState } from 'react'
import { Sidebar } from '../components/Sidebar'
import { Topbar } from '../components/Topbar'
import { PatientsTable } from '../components/PatientsTable'
import { SummaryCards } from '../components/SummaryCards'
import { PatientForm } from '../components/forms/PatientForm'
import { DeletePatientModal } from '../components/modals/DeletePatientModal'
import {
    PatientFilterPanel,
    type PatientFilterValues,
} from '../components/filters/PatientFilterPanel'
import { useToast } from '../../../shared/components/toast/ToastProvider'
import { deletePatient } from '../../../services/patientService'
import type { Gender, PatientResponse, PatientSearchRequest, PatientStatus } from '../../../types/patient'

const emptyFilters: PatientFilterValues = {
    gender: undefined,
    status: undefined,
}

export function PatientsPage() {
    const [isFormOpen, setIsFormOpen] = useState(false)
    const [isFilterOpen, setIsFilterOpen] = useState(false)
    const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false)
    const [isDeleting, setIsDeleting] = useState(false)
    const [formMode, setFormMode] = useState<'create' | 'update'>('create')
    const [selectedPatient, setSelectedPatient] = useState<PatientResponse | null>(null)
    const [refreshKey, setRefreshKey] = useState(0)
    const [searchText, setSearchText] = useState('')

    // Bộ lọc "draft" — chỉ áp dụng khi user nhấn "Áp dụng".
    const [draftFilters, setDraftFilters] = useState<PatientFilterValues>(emptyFilters)
    // Bộ lọc "applied" — dùng cho PatientsTable.getPatients().
    const [appliedFilters, setAppliedFilters] = useState<PatientFilterValues>(emptyFilters)

    const { showToast } = useToast()

    const activeSearch = useMemo<PatientSearchRequest>(() => {
        const request: PatientSearchRequest = {}
        const keyword = searchText.trim()
        // Thanh search hoạt động như một ô tìm "tên HOẶC CCCD HOẶC SĐT":
        // cùng một chuỗi được gửi vào cả 3 field để backend ghép OR (LIKE %x%).
        if (keyword) {
            request.fullName = keyword
            request.nationalId = keyword
            request.phoneNumber = keyword
        }
        if (appliedFilters.gender) request.gender = appliedFilters.gender
        if (appliedFilters.status) request.status = appliedFilters.status
        return request
    }, [appliedFilters, searchText])

    const openCreateForm = () => {
        setSelectedPatient(null)
        setFormMode('create')
        setIsFormOpen(true)
    }

    const openUpdateForm = (patient: PatientResponse) => {
        setSelectedPatient(patient)
        setFormMode('update')
        setIsFormOpen(true)
    }

    const handleSaved = () => {
        setRefreshKey((current) => current + 1)
    }

    const handleDelete = (patient: PatientResponse) => {
        setSelectedPatient(patient)
        setIsDeleteModalOpen(true)
    }

    const confirmDelete = async () => {
        if (!selectedPatient || isDeleting) {
            return
        }

        setIsDeleting(true)
        try {
            const response = await deletePatient(selectedPatient.id)
            showToast('success', 'Thành công', response.message ?? 'Xoá bệnh nhân thành công.')
            setRefreshKey((current) => current + 1)
        } catch (error) {
            showToast('error', 'Thất bại', error instanceof Error ? error.message : 'Xoá bệnh nhân không thành công.')
        } finally {
            setIsDeleting(false)
            setIsDeleteModalOpen(false)
            setSelectedPatient(null)
        }
    }

    const handleSearch = () => {
        setRefreshKey((current) => current + 1)
    }

    const handleApplyFilters = () => {
        const next: PatientFilterValues = {
            gender: (draftFilters.gender || undefined) as Gender | undefined,
            status: (draftFilters.status || undefined) as PatientStatus | undefined,
        }
        setAppliedFilters(next)
        setRefreshKey((current) => current + 1)
        setIsFilterOpen(false)
    }

    const handleResetFilters = () => {
        setDraftFilters(emptyFilters)
        setAppliedFilters(emptyFilters)
        setRefreshKey((current) => current + 1)
        setIsFilterOpen(false)
    }

    const handleChangeFilter = (field: keyof PatientFilterValues, raw: string) => {
        setDraftFilters((current) => ({
            ...current,
            [field]: raw === '' ? undefined : (raw as PatientFilterValues[typeof field]),
        }))
    }

    const isFilterActive = Boolean(appliedFilters.gender) || Boolean(appliedFilters.status)

    return (
        <div className="patients-app">
            <Sidebar />
            <div className="patients-main">
                <Topbar
                    searchText={searchText}
                    onSearchTextChange={setSearchText}
                    onSearch={handleSearch}
                />
                <main className="patients-content">
                    <div className="page-header">
                        <div>
                            <h1>Danh sách Bệnh nhân</h1>
                            <p>Quản lý và theo dõi thông tin bệnh nhân trong hệ thống.</p>
                        </div>
                        <button type="button" className="primary-button" onClick={openCreateForm}>
                            <span className="material-symbols-outlined">person_add</span>
                            Thêm bệnh nhân mới
                        </button>
                    </div>

                    <SummaryCards />

                    {isFilterOpen ? (
                        <PatientFilterPanel
                            values={draftFilters}
                            onChange={handleChangeFilter}
                            onApply={handleApplyFilters}
                            onReset={handleResetFilters}
                        />
                    ) : null}

                    <PatientsTable
                        refreshKey={refreshKey}
                        search={activeSearch}
                        isFilterActive={isFilterActive}
                        onToggleFilter={() => setIsFilterOpen((current) => !current)}
                        onEdit={openUpdateForm}
                        onDelete={handleDelete}
                    />
                </main>
            </div>

            {isFormOpen ? (
                <PatientForm
                    mode={formMode}
                    patient={selectedPatient}
                    onClose={() => setIsFormOpen(false)}
                    onSaved={handleSaved}
                />
            ) : null}

            {isDeleteModalOpen && selectedPatient ? (
                <DeletePatientModal
                    patientName={selectedPatient.fullName}
                    isDeleting={isDeleting}
                    onCancel={() => {
                        if (isDeleting) return
                        setIsDeleteModalOpen(false)
                        setSelectedPatient(null)
                    }}
                    onConfirm={confirmDelete}
                />
            ) : null}
        </div>
    )
}
