export enum Gender {
  MALE = 'MALE',
  FEMALE = 'FEMALE',
  OTHER = 'OTHER',
  UNKNOWN = 'UNKNOWN'
}

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

export interface PageResponse<T> {
  meta: PageMeta;
  result: T[];
}

export interface PageMeta {
  page: number;
  pageSize: number;
  pages: number;
  total: number;
}

export interface ApiResponse<T> {
  statusCode: number;
  message: string;
  data: T;
  error: string | null;
  details: string[] | null;
}
