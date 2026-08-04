import type { ChangeEvent } from 'react'

interface Option {
    value: string
    label: string
}

interface PatientFormFieldProps {
    label: string
    name: string
    value: string
    type?: 'text' | 'date' | 'select' | 'textarea'
    placeholder?: string
    required?: boolean
    icon?: string
    options?: Option[]
    onChange: (event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => void
}

export function PatientFormField({
    label,
    name,
    value,
    type = 'text',
    placeholder,
    required = false,
    icon,
    options,
    onChange,
}: PatientFormFieldProps) {
    const commonClassName = 'form-control'

    return (
        <label className="form-field">
            <span className="form-field-label">
                {icon ? <span className="material-symbols-outlined">{icon}</span> : null}
                <span>{label}</span>
                {required ? <span className="required-dot">*</span> : null}
            </span>

            {type === 'textarea' ? (
                <textarea
                    name={name}
                    value={value}
                    placeholder={placeholder}
                    className={`${commonClassName} form-textarea`}
                    onChange={onChange}
                />
            ) : type === 'select' ? (
                <select name={name} value={value} className={commonClassName} onChange={onChange}>
                    {options?.map((option) => (
                        <option key={option.value} value={option.value}>
                            {option.label}
                        </option>
                    ))}
                </select>
            ) : (
                <input
                    name={name}
                    value={value}
                    type={type}
                    placeholder={placeholder}
                    className={commonClassName}
                    onChange={onChange}
                    required={required}
                />
            )}
        </label>
    )
}
