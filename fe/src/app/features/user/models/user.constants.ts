import { UserStatus, USER_STATUS_OPTIONS } from '../../../shared/enums';

export const USER_STATUS_OPTIONS_ARRAY = USER_STATUS_OPTIONS;
export const USER_STATUS_MAP = new Map<UserStatus, string>(
  USER_STATUS_OPTIONS.map(opt => [opt.value, opt.label])
);
