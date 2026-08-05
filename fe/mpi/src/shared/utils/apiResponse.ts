export function extractApiMessage(payload: unknown, fallback = 'Thao tác đã được xử lý thành công.') {
    if (!payload || typeof payload !== 'object') {
        return fallback
    }

    const response = payload as {
        message?: string
        details?: unknown
        data?: {
            message?: string
        }
    }

    // MethodArgumentNotValidException, chi tiết hơn `message` chung ("Dữ liệu không hợp lệ").
    const detailsMessage = extractDetailsMessage(response.details)
    if (detailsMessage) {
        return detailsMessage
    }

    return response.message ?? response.data?.message ?? fallback
}

/** Gộp mảng `details` từ `ApiResponse` lỗi (vd: validation) thành 1 chuỗi. Trả về undefined nếu rỗng/không hợp lệ. */
function extractDetailsMessage(details: unknown): string | undefined {
    if (!Array.isArray(details) || details.length === 0) {
        return undefined
    }

    const messages = details
        .filter((item): item is string => typeof item === 'string' && item.trim().length > 0)

    if (messages.length === 0) {
        return undefined
    }

    return messages.join('\n')
}
