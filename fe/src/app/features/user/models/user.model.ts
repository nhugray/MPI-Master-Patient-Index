import { Gender, UserStatus } from '../../../shared/enums';
import { RoleInfo } from '../../../shared/models';

export { RoleInfo };


export interface User {
  id: number;
  name: string;
  email: string;
  address: string | null;
  gender: Gender;
  avatar: string | null;
  status: UserStatus;
  roles: RoleInfo[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateUserRequest {
  name: string;
  email: string;
  password: string;
  address?: string | null;
  gender?: Gender;
  avatar?: string | null;
  status?: UserStatus;
  roleIds?: number[];
}

export interface UpdateUserRequest extends Omit<CreateUserRequest, 'password'> {
  id: number;
  password?: string;
}
