import type { ReactNode } from 'react'

interface PatientFormSectionProps {
    title: string
    icon: string
    children: ReactNode
}

export function PatientFormSection({ title, icon, children }: PatientFormSectionProps) {
    return (
        <section className="form-section">
            <div className="form-section-header">
                <span className="material-symbols-outlined">{icon}</span>
                <h3>{title}</h3>
            </div>
            <div className="form-section-grid">{children}</div>
        </section>
    )
}
