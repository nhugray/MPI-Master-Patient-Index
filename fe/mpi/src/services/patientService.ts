import type {
    ApiResponse,
    CreatePatientRequest,
    PatientResponse,
    PatientSearchRequest,
    PatientsApiResponse,
    UpdatePatientRequest,
} from '../types/patient'
import { extractApiMessage } from '../shared/utils/apiResponse'

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'

async function request<T>(url: string, options?: RequestInit): Promise<T> {
    const response = await fetch(`${API_BASE}${url}`, {
        headers: {
            'Content-Type': 'application/json',
        },
        ...options,
    })

    const payload = (await response.json().catch(() => null)) as T | null

    if (!response.ok) {
        throw new Error(extractApiMessage(payload, 'Xử lý không thành công.'))
    }

    return payload as T
}

export interface PatientsQuery extends PatientSearchRequest {
    /** Trang hiện tại (1-based cho FE) — sẽ được chuyển về 0-based khi gọi API. */
    page?: number
    size?: number
}

/** Kích thước trang mặc định khi không truyền. */
export const DEFAULT_PAGE_SIZE = 5

/** Các kích thước trang được phép hiển thị trong UI. */
export const PAGE_SIZE_OPTIONS = [5, 10, 20, 50] as const

export async function getPatients(query?: PatientsQuery) {
    const params = new URLSearchParams()
    const size = query?.size ?? DEFAULT_PAGE_SIZE
    // Spring Pageable nhận `page` 0-based, nhưng UI dùng 1-based (meta.page cũng 1-based).
    const pageNumber0Based = Math.max(0, (query?.page ?? 1) - 1)

    if (query?.fullName) params.set('fullName', query.fullName)
    if (query?.gender) params.set('gender', query.gender)
    if (query?.nationalId) params.set('nationalId', query.nationalId)
    if (query?.healthInsuranceNo) params.set('healthInsuranceNo', query.healthInsuranceNo)
    if (query?.phoneNumber) params.set('phoneNumber', query.phoneNumber)
    if (query?.status) params.set('status', query.status)

    params.set('page', String(pageNumber0Based))
    params.set('size', String(size))

    const payload = await request<ApiResponse<PatientsApiResponse>>(`/patients?${params.toString()}`)

    return {
        content: payload.data.result as PatientResponse[],
        meta: payload.data.meta,
    }
}

export async function createPatient(payload: CreatePatientRequest) {
    return request<ApiResponse<PatientResponse>>('/patients', {
        method: 'POST',
        body: JSON.stringify(payload),
    })
}

export async function updatePatient(payload: UpdatePatientRequest) {
    return request<ApiResponse<PatientResponse>>('/patients', {
        method: 'PUT',
        body: JSON.stringify(payload),
    })
}

export async function deletePatient(id: number) {
    return request<ApiResponse<void>>(`/patients/${id}`, {
        method: 'DELETE',
    })
}
