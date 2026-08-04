export function extractApiMessage(payload: unknown, fallback = 'Thao tác đã được xử lý thành công.') {
    if (!payload || typeof payload !== 'object') {
        return fallback
    }

    const response = payload as {
        message?: string
        data?: {
            message?: string
        }
    }

    return response.message ?? response.data?.message ?? fallback
}
