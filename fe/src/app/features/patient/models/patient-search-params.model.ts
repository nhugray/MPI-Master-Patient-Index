import { Gender, MatchStatus } from '../../../shared/enums';
import { PatientStatus } from './patient.model';

export { Gender, MatchStatus, PatientStatus };

export interface PatientSearchParams {
  fullName?: string;
  gender?: Gender;
  nationalId?: string;
  healthInsuranceNo?: string;
  phoneNumber?: string;
  address?: string;
  sourceSystemId?: number;
  localPatientCode?: string;
  matchStatus?: MatchStatus;
  page?: number;
  size?: number;
}

export const defaultSearchParams: PatientSearchParams = {
  page: 0,
  size: 10
};
