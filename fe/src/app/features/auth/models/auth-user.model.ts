import { UserStatus, UserRole } from '../../../shared/enums';
import { RoleInfo } from '../../../shared/models';

export { UserRole, RoleInfo };

export interface AuthUser {
  id: number;
  name: string;
  email: string;
  address?: string | null;
  gender?: string;
  avatar?: string;
  status?: UserStatus;
  roles: RoleInfo[];
  createdAt?: string;
  updatedAt?: string;
}
