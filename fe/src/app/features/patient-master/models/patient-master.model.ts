export interface PatientMaster {
  id: number;
  enterpriseId: string; // EMPI
  fullName: string;
  dateOfBirth: string;
  gender: string;
  nationalId?: string;
  healthInsuranceNo?: string;
  phoneNumber?: string;
  address?: string;
  status: string; // ACTIVE, INACTIVE, MERGED
  linkedPatientCount?: number;
  createdAt: string;
  updatedAt: string;
}

export interface PatientMasterSearchRequest {
  keyword?: string;
  gender?: string;
  status?: string;
  ageFrom?: number;
  ageTo?: number;
  sourceSystemIds?: number[];
  page: number;
  size: number;
}
