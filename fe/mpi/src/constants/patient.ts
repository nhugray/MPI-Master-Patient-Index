import type { Gender, PatientStatus } from '../types/patient'

export const genderLabels: Record<Gender, string> = {
    MALE: 'Nam',
    FEMALE: 'Nữ',
    OTHER: 'Khác',
    UNKNOWN: 'Chưa rõ',
}

export const statusLabels: Record<PatientStatus, string> = {
    ACTIVE: 'Đang điều trị',
    MERGED: 'Đã xuất viện',
    DECEASED: 'Đã mất',
    INACTIVE: 'Chờ khám',
}

export const statusTone: Record<PatientStatus, string> = {
    ACTIVE: 'status-active',
    MERGED: 'status-merged',
    DECEASED: 'status-deceased',
    INACTIVE: 'status-inactive',
}
