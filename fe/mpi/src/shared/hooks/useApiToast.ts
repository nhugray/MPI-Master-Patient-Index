import { useCallback } from 'react'
import { useToast } from '../components/toast/ToastProvider'

interface ApiToastResult {
    successTitle?: string
    errorTitle?: string
}

export function useApiToast() {
    const { showToast } = useToast()

    const runWithToast = useCallback(
        async <T>(
            request: () => Promise<T>,
            options?: ApiToastResult,
        ): Promise<T | null> => {
            try {
                const response = await request()
                showToast(
                    'success',
                    options?.successTitle ?? 'Thành công',
                    typeof response === 'string' ? response : 'Thao tác đã được xử lý thành công.',
                )
                return response
            } catch (error) {
                const message = error instanceof Error ? error.message : 'Đã xảy ra lỗi không xác định.'
                showToast('error', options?.errorTitle ?? 'Thất bại', message)
                return null
            }
        },
        [showToast],
    )

    return { runWithToast }
}
