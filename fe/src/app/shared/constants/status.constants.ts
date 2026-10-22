export interface ActiveStatusOption {
  value: boolean;
  label: string;
}

export const ACTIVE_STATUS_OPTIONS: ActiveStatusOption[] = [
  { value: true, label: 'Đang hoạt động' },
  { value: false, label: 'Ngừng hoạt động' }
];

export const STATUS_OPTIONS = ACTIVE_STATUS_OPTIONS;
