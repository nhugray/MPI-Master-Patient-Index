import { Gender } from '../../../shared/enums';

export { Gender };

export enum PatientStatus {
  ACTIVE = 'ACTIVE',
  MERGED = 'MERGED',
  DECEASED = 'DECEASED',
  INACTIVE = 'INACTIVE'
}

export interface Patient {
  id: number;
  fullName: string;
  dateOfBirth: string | null;
  gender: Gender;
  nationalId: string;
  phoneNumber: string;
  status: PatientStatus;
  healthInsuranceNo: string | null;
}

export interface CreatePatientRequest {
  fullName: string;
  dateOfBirth: string | null;
  gender: Gender;
  nationalId: string;
  phoneNumber: string;
  status: PatientStatus;
  healthInsuranceNo: string | null;
}

export interface UpdatePatientRequest extends CreatePatientRequest {
  id: number;
}
