import { useMemo } from 'react'
import './Pagination.css'

export interface PaginationProps {
    currentPage: number
    totalPages: number
    onPageChange: (page: number) => void
    isLoading?: boolean
    pageSize?: number
    pageSizeOptions?: readonly number[]
    onPageSizeChange?: (size: number) => void
    showPageIndicator?: boolean
}

const buildPageItems = (currentPage: number, totalPages: number): Array<number | '…'> => {
    if (totalPages <= 0) return []
    if (totalPages === 1) return [1]

    const siblings = 1
    const totalSlots = 5 + siblings * 2

    // Nếu tổng trang nhỏ, hiển thị tất cả.
    if (totalPages <= totalSlots) {
        return Array.from({ length: totalPages }, (_, i) => i + 1)
    }

    const leftSibling = Math.max(currentPage - siblings, 2)
    const rightSibling = Math.min(currentPage + siblings, totalPages - 1)

    const showLeftEllipsis = leftSibling > 2
    const showRightEllipsis = rightSibling < totalPages - 1

    const firstPage = 1
    const lastPage = totalPages
    const items: Array<number | '…'> = [firstPage]

    // Vùng giữa: leftSibling .. rightSibling (không tính firstPage / lastPage).
    for (let page = leftSibling; page <= rightSibling; page++) {
        items.push(page)
    }

    if (showLeftEllipsis) {
        items.push('…')
    }

    if (showRightEllipsis) {
        items.push('…')
    }

    if (lastPage !== firstPage) {
        items.push(lastPage)
    }

    return items
}

export function Pagination({
    currentPage,
    totalPages,
    onPageChange,
    isLoading = false,
    pageSize,
    pageSizeOptions,
    onPageSizeChange,
    showPageIndicator = true,
}: PaginationProps) {
    const pages = useMemo(
        () => {
            const raw = buildPageItems(currentPage, totalPages)
            const seen = new Set<number>()
            return raw.filter((item) => {
                if (item === '…') return true
                if (seen.has(item)) return false
                seen.add(item)
                return true
            })
        },
        [currentPage, totalPages],
    )

    if (totalPages === 0) return null

    const goTo = (page: number) => {
        const clamped = Math.min(Math.max(1, page), totalPages)
        if (clamped === currentPage || isLoading) return
        onPageChange(clamped)
    }

    const isFirst = currentPage <= 1
    const isLast = currentPage >= totalPages

    return (
        <nav className="pagination-bar" aria-label="Phân trang">
            <div className="pagination-nav">
                <button
                    type="button"
                    className="page-button page-chevron"
                    aria-label="Về trang đầu"
                    disabled={isFirst || isLoading}
                    onClick={() => goTo(1)}
                >
                    <span className="material-symbols-outlined">first_page</span>
                </button>
                <button
                    type="button"
                    className="page-button page-chevron"
                    aria-label="Trang trước"
                    disabled={isFirst || isLoading}
                    onClick={() => goTo(currentPage - 1)}
                >
                    <span className="material-symbols-outlined">chevron_left</span>
                </button>

                <ul className="page-list">
                    {pages.map((item, index) =>
                        item === '…' ? (
                            <li key={`ellipsis-${index}`} aria-hidden="true" className="page-ellipsis">
                                …
                            </li>
                        ) : (
                            <li key={item}>
                                <button
                                    type="button"
                                    className={`page-button${item === currentPage ? ' active' : ''}`}
                                    aria-current={item === currentPage ? 'page' : undefined}
                                    aria-label={`Trang ${item}`}
                                    disabled={isLoading}
                                    onClick={() => goTo(item)}
                                >
                                    {item}
                                </button>
                            </li>
                        ),
                    )}
                </ul>

                <button
                    type="button"
                    className="page-button page-chevron"
                    aria-label="Trang sau"
                    disabled={isLast || isLoading}
                    onClick={() => goTo(currentPage + 1)}
                >
                    <span className="material-symbols-outlined">chevron_right</span>
                </button>
                <button
                    type="button"
                    className="page-button page-chevron"
                    aria-label="Đến trang cuối"
                    disabled={isLast || isLoading}
                    onClick={() => goTo(totalPages)}
                >
                    <span className="material-symbols-outlined">last_page</span>
                </button>
            </div>

            {showPageIndicator && totalPages > 0 ? (
                <span className="page-indicator" aria-live="polite">
                    Trang <strong>{currentPage}</strong> / <strong>{totalPages}</strong>
                </span>
            ) : null}

            {pageSizeOptions && pageSize !== undefined && onPageSizeChange ? (
                <label className="page-size-picker">
                    <span>Mỗi trang:</span>
                    <select
                        value={pageSize}
                        onChange={(event) => onPageSizeChange(Number(event.target.value))}
                        disabled={isLoading}
                        aria-label="Số bệnh nhân trên một trang"
                    >
                        {pageSizeOptions.map((size) => (
                            <option key={size} value={size}>
                                {size}
                            </option>
                        ))}
                    </select>
                </label>
            ) : null}
        </nav>
    )
}
