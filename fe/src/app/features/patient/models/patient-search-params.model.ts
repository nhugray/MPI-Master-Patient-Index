import { Gender } from '../../../shared/enums';
import { PatientStatus } from './patient.model';

export { Gender, PatientStatus };

export interface PatientSearchParams {
  fullName?: string;
  gender?: Gender;
  nationalId?: string;
  healthInsuranceNo?: string;
  phoneNumber?: string;
  status?: PatientStatus;
  page?: number;
  size?: number;
}

export const defaultSearchParams: PatientSearchParams = {
  page: 0,
  size: 10
};
