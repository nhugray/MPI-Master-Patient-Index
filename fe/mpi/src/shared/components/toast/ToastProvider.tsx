import { createContext, useContext, useMemo, useState } from 'react'
import type { ReactNode } from 'react'

export type ToastType = 'success' | 'error'

interface ToastItem {
    id: number
    type: ToastType
    title: string
    message: string
}

interface ToastContextValue {
    showToast: (type: ToastType, title: string, message: string) => void
    hideToast: (id: number) => void
}

const ToastContext = createContext<ToastContextValue | null>(null)

export function ToastProvider({ children }: { children: ReactNode }) {
    const [toasts, setToasts] = useState<ToastItem[]>([])

    const showToast = (type: ToastType, title: string, message: string) => {
        const id = Date.now() + Math.floor(Math.random() * 1000)
        const nextToast: ToastItem = { id, type, title, message }

        setToasts((current) => [...current, nextToast])

        window.setTimeout(() => {
            setToasts((current) => current.filter((toast) => toast.id !== id))
        }, 3000)
    }

    const hideToast = (id: number) => {
        setToasts((current) => current.filter((toast) => toast.id !== id))
    }

    const value = useMemo(() => ({ showToast, hideToast }), [])

    return (
        <ToastContext.Provider value={value}>
            {children}
            <div className="toast-viewport" aria-live="polite" aria-atomic="true">
                {toasts.map((toast) => (
                    <div
                        key={toast.id}
                        className={`toast-card ${toast.type}`}
                        role="status"
                    >
                        <div className="toast-icon-wrap">
                            <span className="material-symbols-outlined">
                                {toast.type === 'success' ? 'check_circle' : 'error'}
                            </span>
                        </div>

                        <div className="toast-copy">
                            <strong>{toast.title}</strong>
                            <span>{toast.message}</span>
                        </div>

                        <button type="button" className="toast-close" onClick={() => hideToast(toast.id)}>
                            <span className="material-symbols-outlined">close</span>
                        </button>
                    </div>
                ))}
            </div>
        </ToastContext.Provider>
    )
}

export function useToast() {
    const toastContext = useContext(ToastContext)

    if (!toastContext) {
        throw new Error('useToast must be used within ToastProvider')
    }

    return toastContext
}
