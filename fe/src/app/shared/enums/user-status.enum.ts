export enum UserStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  SUSPENDED = 'SUSPENDED'
}

export interface UserStatusOption {
  value: UserStatus;
  label: string;
  icon: string;
  color: string;
}

export const USER_STATUS_OPTIONS: UserStatusOption[] = [
  {
    value: UserStatus.ACTIVE,
    label: 'Đang hoạt động',
    icon: 'check_circle',
    color: '#10b981'
  },
  {
    value: UserStatus.INACTIVE,
    label: 'Không hoạt động',
    icon: 'cancel',
    color: '#6b7280'
  },
  {
    value: UserStatus.SUSPENDED,
    label: 'Bị khóa',
    icon: 'lock',
    color: '#ef4444'
  }
];

export const USER_STATUS_ALL_OPTION = { value: 'ALL', label: 'Tất cả trạng thái' };
