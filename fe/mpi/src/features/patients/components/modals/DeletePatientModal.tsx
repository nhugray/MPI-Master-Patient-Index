/**
 * DeletePatientModal — xác nhận xoá bệnh nhân.
 *
 * Được thiết kế theo form mẫu fe/mpi/form-delete.html (icon ưu tiên đỏ 2 lớp,
 * thông điệp với tên bệnh nhân, nút Huỷ + nút Xác nhận xoá đỏ), nhưng ánh xạ
 * sang hệ CSS module riêng và bám tokens thương hiệu của dự án (#d03939, font
 * Inter/Manrope, bán kính 18px) để giữ sự nhất quán với các modal khác.
 *
 * Logic bên ngoài trong PatientsPage xử lý API + toast + refresh; component
 * này chỉ là lớp hiển thị + điều phối cancel/confirm.
 */
import { useEffect, useState } from 'react'
import './DeletePatientModal.css'

interface DeletePatientModalProps {
    patientName: string
    isDeleting?: boolean
    onCancel: () => void
    onConfirm: () => void | Promise<void>
}

export function DeletePatientModal({ patientName, isDeleting = false, onCancel, onConfirm }: DeletePatientModalProps) {
    const [submitting, setSubmitting] = useState(false)

    useEffect(() => {
        const handleKey = (event: KeyboardEvent) => {
            if (event.key === 'Escape' && !submitting && !isDeleting) {
                onCancel()
            }
        }
        document.addEventListener('keydown', handleKey)
        const previousOverflow = document.body.style.overflow
        document.body.style.overflow = 'hidden'
        return () => {
            document.removeEventListener('keydown', handleKey)
            document.body.style.overflow = previousOverflow
        }
    }, [onCancel, submitting, isDeleting])

    const handleConfirm = async () => {
        if (submitting || isDeleting) return
        setSubmitting(true)
        try {
            await onConfirm()
        } finally {
            setSubmitting(false)
        }
    }

    const handleBackdropMouseDown = (event: React.MouseEvent<HTMLDivElement>) => {
        if (event.target === event.currentTarget && !submitting && !isDeleting) {
            onCancel()
        }
    }

    const busy = submitting || isDeleting

    return (
        <div
            className="delete-modal-overlay"
            role="dialog"
            aria-modal="true"
            aria-labelledby="delete-modal-title"
            aria-describedby="delete-modal-description"
            onMouseDown={handleBackdropMouseDown}
        >
            <div className="delete-modal-card" onMouseDown={(event) => event.stopPropagation()}>
                <div className="delete-modal-icon" aria-hidden="true">
                    <span className="delete-modal-icon-inner">
                        <span
                            className="material-symbols-outlined"
                            style={{ fontVariationSettings: "'FILL' 1, 'wght' 600" }}
                        >
                            priority_high
                        </span>
                    </span>
                </div>

                <div className="delete-modal-body">
                    <h3 id="delete-modal-title" className="delete-modal-title">
                        Xác nhận xoá bệnh nhân
                    </h3>
                    <p id="delete-modal-description" className="delete-modal-description">
                        Bạn có chắc chắn muốn xoá bệnh nhân <strong>{patientName}</strong> khỏi hệ thống?
                        <span className="delete-modal-warning">Hành động này không thể hoàn tác.</span>
                    </p>
                </div>

                <div className="delete-modal-actions">
                    <button
                        type="button"
                        className="delete-modal-cancel"
                        onClick={onCancel}
                        disabled={busy}
                    >
                        Huỷ
                    </button>
                    <button
                        type="button"
                        className="delete-modal-confirm"
                        onClick={handleConfirm}
                        disabled={busy}
                        aria-busy={busy}
                    >
                        {busy ? (
                            <>
                                <span className="delete-modal-spinner" aria-hidden="true" />
                                Đang xoá...
                            </>
                        ) : (
                            <>
                                <span
                                    className="material-symbols-outlined"
                                    style={{ fontSize: 18, marginRight: 4 }}
                                    aria-hidden="true"
                                >
                                    delete
                                </span>
                                Xác nhận xoá
                            </>
                        )}
                    </button>
                </div>
            </div>
        </div>
    )
}
