export enum UserRole {
  ADMIN = 'ADMIN',
  REVIEWER = 'REVIEWER',
  VIEWER = 'VIEWER'
}

export interface UserRoleOption {
  value: UserRole;
  label: string;
  icon: string;
  description: string;
}

export const USER_ROLE_OPTIONS: UserRoleOption[] = [
  {
    value: UserRole.ADMIN,
    label: 'Quản trị viên',
    icon: 'admin_panel_settings',
    description: 'Toàn quyền hệ thống'
  },
  {
    value: UserRole.REVIEWER,
    label: 'Người duyệt',
    icon: 'verified_user',
    description: 'Duyệt & đối soát dữ liệu'
  },
  {
    value: UserRole.VIEWER,
    label: 'Người xem',
    icon: 'visibility',
    description: 'Chỉ xem thông tin'
  }
];

export const USER_ROLE_ALL_OPTION = { value: 'ALL', label: 'Tất cả vai trò' };
