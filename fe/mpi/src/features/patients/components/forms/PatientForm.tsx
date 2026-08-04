import { useEffect, useMemo, useState } from 'react'
import { genderLabels, statusLabels } from '../../../../constants/patient'
import { createPatient, updatePatient } from '../../../../services/patientService'
import { useToast } from '../../../../shared/components/toast/ToastProvider'
import type { Gender, PatientResponse, PatientStatus } from '../../../../types/patient'
import { PatientFormField } from './PatientFormField'
import { PatientFormSection } from './PatientFormSection'

interface PatientFormProps {
    mode: 'create' | 'update'
    patient?: PatientResponse | null
    onClose: () => void
    onSaved: () => void
}

interface PatientFormValues {
    fullName: string
    dateOfBirth: string
    gender: Gender
    nationalId: string
    healthInsuranceNo: string
    phoneNumber: string
    status: PatientStatus
    note: string
    carePlan: string
    visitSlot: string
}

const initialValues: PatientFormValues = {
    fullName: '',
    dateOfBirth: '',
    gender: 'MALE',
    nationalId: '',
    healthInsuranceNo: '',
    phoneNumber: '',
    status: 'ACTIVE',
    note: 'Bệnh nhân cần theo dõi thêm dấu hiệu sinh tồn và lịch tái khám định kỳ.',
    carePlan: 'Phác đồ theo dõi bệnh mãn tính',
    visitSlot: 'Sáng - 08:30',
}

export function PatientForm({ mode, patient, onClose, onSaved }: PatientFormProps) {
    const [values, setValues] = useState<PatientFormValues>(initialValues)
    const [isSubmitting, setIsSubmitting] = useState(false)
    const { showToast } = useToast()

    useEffect(() => {
        if (patient) {
            setValues({
                fullName: patient.fullName,
                dateOfBirth: patient.dateOfBirth,
                gender: patient.gender,
                nationalId: patient.nationalId,
                healthInsuranceNo: patient.healthInsuranceNo ?? '',
                phoneNumber: patient.phoneNumber ?? '',
                status: patient.status,
                note: 'Cần theo dõi thêm chỉ số sinh tồn và uống thuốc theo giờ.',
                carePlan: 'Phác đồ chăm sóc theo từng giai đoạn điều trị',
                visitSlot: 'Sáng - 08:30',
            })
        } else {
            setValues(initialValues)
        }
    }, [patient])

    const title = useMemo(() => (mode === 'create' ? 'Thêm bệnh nhân mới' : 'Cập nhật hồ sơ bệnh nhân'), [mode])

    const handleChange = (
        event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>,
    ) => {
        const { name, value } = event.target
        setValues((current) => ({ ...current, [name]: value }))
    }

    const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
        event.preventDefault()
        setIsSubmitting(true)

        try {
            const payload = {
                fullName: values.fullName,
                dateOfBirth: values.dateOfBirth,
                gender: values.gender,
                nationalId: values.nationalId,
                healthInsuranceNo: values.healthInsuranceNo,
                phoneNumber: values.phoneNumber,
                status: values.status,
            }

            let response

            if (mode === 'create') {
                response = await createPatient(payload)
            } else if (patient?.id) {
                response = await updatePatient({ ...payload, id: patient.id })
            }

            showToast('success', 'Thành công', response?.message ?? 'Thao tác đã được xử lý thành công.')
            onSaved()
            onClose()
        } catch (error) {
            showToast('error', 'Thất bại', error instanceof Error ? error.message : 'Đã xảy ra lỗi không xác định.')
        } finally {
            setIsSubmitting(false)
        }
    }

    return (
        <div className="form-modal-backdrop" onClick={onClose}>
            <div className="form-modal-panel" onClick={(event) => event.stopPropagation()}>
                <div className="form-modal-header">
                    <div>
                        <span className="material-symbols-outlined">person_add</span>
                        <div>
                            <h2>{title}</h2>
                            <p>Quản lý hồ sơ theo chuẩn dữ liệu y tế hiện tại.</p>
                        </div>
                    </div>

                    <button type="button" className="icon-button" onClick={onClose} aria-label="Đóng form">
                        <span className="material-symbols-outlined">close</span>
                    </button>
                </div>

                <form className="form-modal-body" onSubmit={handleSubmit}>
                    <PatientFormSection title="Thông tin cơ bản" icon="assignment_ind">
                        <PatientFormField
                            label="Họ và tên"
                            name="fullName"
                            value={values.fullName}
                            placeholder="VD: Nguyễn Văn A"
                            icon="person"
                            required
                            onChange={handleChange}
                        />
                        <PatientFormField
                            label="Ngày sinh"
                            name="dateOfBirth"
                            value={values.dateOfBirth}
                            type="date"
                            placeholder="Chọn ngày"
                            icon="calendar_today"
                            required
                            onChange={handleChange}
                        />
                        <PatientFormField
                            label="Giới tính"
                            name="gender"
                            value={values.gender}
                            type="select"
                            icon="wc"
                            onChange={handleChange}
                            options={Object.entries(genderLabels).map(([value, label]) => ({ value, label }))}
                        />
                        <PatientFormField
                            label="CCCD / CMND"
                            name="nationalId"
                            value={values.nationalId}
                            placeholder="12 chữ số"
                            icon="badge"
                            required
                            onChange={handleChange}
                        />
                    </PatientFormSection>

                    <PatientFormSection title="Định danh & bảo hiểm" icon="shield">
                        <PatientFormField
                            label="Số BHYT"
                            name="healthInsuranceNo"
                            value={values.healthInsuranceNo}
                            placeholder="Không bắt buộc"
                            icon="local_hospital"
                            onChange={handleChange}
                        />
                        <PatientFormField
                            label="Số điện thoại"
                            name="phoneNumber"
                            value={values.phoneNumber}
                            placeholder="VD: 0987654321"
                            icon="call"
                            required
                            onChange={handleChange}
                        />
                        <PatientFormField
                            label="Trạng thái"
                            name="status"
                            value={values.status}
                            type="select"
                            icon="monitor_heart"
                            onChange={handleChange}
                            options={Object.entries(statusLabels).map(([value, label]) => ({ value, label }))}
                        />
                        <PatientFormField
                            label="Khung giờ khám"
                            name="visitSlot"
                            value={values.visitSlot}
                            placeholder="VD: Sáng - 08:30"
                            icon="schedule"
                            onChange={handleChange}
                        />
                    </PatientFormSection>

                    <PatientFormSection title="Ghi chú điều trị" icon="note_alt">
                        <PatientFormField
                            label="Ghi chú y tế"
                            name="note"
                            value={values.note}
                            type="textarea"
                            placeholder="Nhập hướng dẫn bổ sung"
                            icon="stethoscope"
                            onChange={handleChange}
                        />
                        <PatientFormField
                            label="Phác đồ chăm sóc"
                            name="carePlan"
                            value={values.carePlan}
                            type="textarea"
                            placeholder="Phác đồ chăm sóc"
                            icon="playlist_add_check_circle"
                            onChange={handleChange}
                        />
                    </PatientFormSection>

                    <div className="form-actions">
                        <button type="button" className="ghost-button" onClick={onClose}>
                            <span className="material-symbols-outlined">close</span>
                            Hủy
                        </button>
                        <button type="submit" className="primary-button" disabled={isSubmitting}>
                            <span className="material-symbols-outlined">save</span>
                            {isSubmitting ? 'Đang lưu...' : mode === 'create' ? 'Thêm bệnh nhân' : 'Cập nhật'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    )
}
