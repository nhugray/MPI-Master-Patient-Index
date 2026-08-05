import { useEffect, useState } from 'react'
import { getPatients } from '../../../services/patientService'

type SummaryCardTone = 'primary' | 'success' | 'warning'

type SummaryCardKey = 'total' | 'active'

interface SummaryCardSpec {
    key: SummaryCardKey
    title: string
    icon: string
    tone: SummaryCardTone
}

/** Cấu hình card — thêm/bớt card chỉ cần chỉnh mảng này + extend SummaryStats. */
const SUMMARY_CARDS_SPEC: readonly SummaryCardSpec[] = [
    { key: 'total', title: 'Tổng bệnh nhân', icon: 'groups', tone: 'primary' },
    { key: 'active', title: 'Đang điều trị', icon: 'medical_services', tone: 'success' },
]

interface SummaryStats {
    total: number | null
    active: number | null
}

const EMPTY_STATS: SummaryStats = { total: null, active: null }

const numberFormatter = new Intl.NumberFormat('vi-VN')

function formatStat(value: number | null, isLoading: boolean): string {
    if (isLoading || value === null) return '—'
    return numberFormatter.format(value)
}

interface SummaryCardsProps {
    /** Bump để trigger refetch (vd: sau create/delete bệnh nhân). */
    refreshKey: number
}

export function SummaryCards({ refreshKey }: SummaryCardsProps) {
    const [stats, setStats] = useState<SummaryStats>(EMPTY_STATS)
    const [isLoading, setIsLoading] = useState(true)

    useEffect(() => {
        let cancelled = false

        const run = async () => {
            setIsLoading(true)
            try {
                // 2 request song song — total từ list không filter,
                // active = filter theo status hiện có (mapping PatientStatusEnum.ACTIVE trong constants).
                const [all, active] = await Promise.all([
                    getPatients({ page: 1, size: 1 }),
                    getPatients({ status: 'ACTIVE', page: 1, size: 1 }),
                ])
                if (cancelled) return
                setStats({ total: all.meta.total, active: active.meta.total })
            } catch {
                if (!cancelled) setStats(EMPTY_STATS)
            } finally {
                if (!cancelled) setIsLoading(false)
            }
        }

        void run()
        return () => {
            cancelled = true
        }
    }, [refreshKey])

    return (
        <section className="summary-grid">
            {SUMMARY_CARDS_SPEC.map((card) => (
                <article key={card.key} className={`summary-card ${card.tone}`}>
                    <div className="summary-icon">
                        <span className="material-symbols-outlined">{card.icon}</span>
                    </div>
                    <div>
                        <div className="summary-value">{formatStat(stats[card.key], isLoading)}</div>
                        <div className="summary-title">{card.title}</div>
                    </div>
                </article>
            ))}
        </section>
    )
}