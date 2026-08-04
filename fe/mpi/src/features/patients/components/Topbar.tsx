interface TopbarProps {
    searchText: string
    onSearchTextChange: (value: string) => void
    onSearch: () => void
}

export function Topbar({ searchText, onSearchTextChange, onSearch }: TopbarProps) {
    return (
        <header className="topbar">
            <div className="topbar-search">
                <span className="material-symbols-outlined">search</span>
                <input
                    value={searchText}
                    onChange={(event) => onSearchTextChange(event.target.value)}
                    onKeyDown={(event) => {
                        if (event.key === 'Enter') {
                            onSearch()
                        }
                    }}
                    placeholder="Tìm theo tên, CCCD hoặc số điện thoại..."
                    aria-label="Tìm bệnh nhân theo tên, CCCD hoặc số điện thoại"
                />
            </div>

            <div className="topbar-actions">
                <button type="button" className="icon-button" aria-label="Thông báo">
                    <span className="material-symbols-outlined">notifications</span>
                </button>
                <button type="button" className="icon-button" aria-label="Trợ giúp">
                    <span className="material-symbols-outlined">help</span>
                </button>

                <div className="profile-pill">
                    <span className="avatar">B</span>
                    <div className="profile-copy">
                        <strong>BS. Nguyễn Khoa</strong>
                        <small>Trưởng khoa Nội</small>
                    </div>
                </div>
            </div>
        </header>
    )
}
