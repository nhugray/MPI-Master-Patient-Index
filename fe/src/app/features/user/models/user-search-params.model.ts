import { UserStatus } from '../../../shared/enums';

export interface UserSearchParams {
  keyword?: string;
  status?: UserStatus;
  roleId?: number;
  page?: number;
  size?: number;
}

export const defaultSearchParams: UserSearchParams = {
  page: 0,
  size: 10
};
