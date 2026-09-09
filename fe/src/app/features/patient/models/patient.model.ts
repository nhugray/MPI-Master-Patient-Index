import { Gender, MatchStatus } from '../../../shared/enums';

export { Gender, MatchStatus };

export enum PatientStatus {
  ACTIVE = 'ACTIVE',
  MERGED = 'MERGED',
  DECEASED = 'DECEASED',
  INACTIVE = 'INACTIVE'
}

export interface Patient {
  id: number;
  sourceSystemId: number;
  sourceSystemName: string;
  localPatientCode: string;
  fullName: string;
  dateOfBirth: string | null;
  gender: Gender;
  nationalId: string | null;
  phoneNumber: string | null;
  address: string | null;
  healthInsuranceNo: string | null;
  masterPatientId: number | null;
  matchStatus: MatchStatus;
}

export interface CreatePatientRequest {
  sourceSystemId: number;
  localPatientCode: string;
  fullName: string;
  dateOfBirth: string | null;
  gender: Gender;
  nationalId: string | null;
  phoneNumber: string | null;
  address: string | null;
  healthInsuranceNo: string | null;
}

export interface UpdatePatientRequest extends CreatePatientRequest {
  id: number;
}
