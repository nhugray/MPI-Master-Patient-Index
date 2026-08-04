export function Sidebar() {
    const items = [
        { label: 'Dashboard', icon: 'dashboard' },
        { label: 'Bệnh nhân', icon: 'people', active: true },
        { label: 'Lịch hẹn', icon: 'event' },
        { label: 'Hồ sơ bệnh án', icon: 'description' },
        { label: 'Báo cáo', icon: 'bar_chart' },
    ]

    return (
        <aside className="sidebar">
            <div className="sidebar-brand">
                <div className="logo-pill">
                    <span className="material-symbols-outlined">medical_services</span>
                </div>
                <div className="sidebar-brand-copy">
                    <strong>MPI</strong>
                    <small>Hệ thống quản lý y tế</small>
                </div>
            </div>

            <nav className="sidebar-nav">
                {items.map((item) => (
                    <a
                        key={item.label}
                        href="#"
                        className={`sidebar-link ${item.active ? 'active' : ''}`}
                    >
                        <span className="material-symbols-outlined">{item.icon}</span>
                        <span>{item.label}</span>
                    </a>
                ))}
            </nav>

            <div className="sidebar-footer">
                <a href="#" className="sidebar-link footer-link">
                    <span className="material-symbols-outlined">settings</span>
                    <span>Cài đặt</span>
                </a>
                <a href="#" className="sidebar-link footer-link logout-link">
                    <span className="material-symbols-outlined">logout</span>
                    <span>Đăng xuất</span>
                </a>
            </div>
        </aside>
    )
}
