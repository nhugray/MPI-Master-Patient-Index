const cards = [
    {
        title: 'Tổng bệnh nhân',
        value: '1,284',
        icon: 'groups',
        tone: 'primary',
    },
    {
        title: 'Đang điều trị',
        value: '45',
        icon: 'medical_services',
        tone: 'success',
    },
    {
        title: 'Lượt khám hôm nay',
        value: '12',
        icon: 'calendar_month',
        tone: 'warning',
    },
]

export function SummaryCards() {
    return (
        <section className="summary-grid">
            {cards.map((card) => (
                <article key={card.title} className={`summary-card ${card.tone}`}>
                    <div className="summary-icon">
                        <span className="material-symbols-outlined">{card.icon}</span>
                    </div>
                    <div>
                        <div className="summary-value">{card.value}</div>
                        <div className="summary-title">{card.title}</div>
                    </div>
                </article>
            ))}
        </section>
    )
}
