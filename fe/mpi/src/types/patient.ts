export type Gender = 'MALE' | 'FEMALE' | 'OTHER' | 'UNKNOWN'
export type PatientStatus = 'ACTIVE' | 'MERGED' | 'DECEASED' | 'INACTIVE'

export interface PatientResponse {
    id: number
    fullName: string
    dateOfBirth: string
    gender: Gender
    nationalId: string
    healthInsuranceNo?: string
    phoneNumber?: string
    status: PatientStatus
}

export interface PaginationMeta {
    page: number
    pageSize: number
    pages: number
    total: number
}

export interface ApiResponse<T> {
    statusCode: number
    message: string
    data: T
}

export interface PatientsApiResponse {
    meta: PaginationMeta
    result: PatientResponse[]
}

export interface PatientSearchRequest {
    fullName?: string
    gender?: Gender
    nationalId?: string
    healthInsuranceNo?: string
    phoneNumber?: string
    status?: PatientStatus
}

export interface CreatePatientRequest {
    fullName: string
    dateOfBirth: string
    gender: Gender
    nationalId: string
    healthInsuranceNo?: string
    phoneNumber?: string
    status: PatientStatus
}

export interface UpdatePatientRequest extends CreatePatientRequest {
    id: number
}
