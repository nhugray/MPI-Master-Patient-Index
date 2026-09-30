export interface PatientMasterDetail {
  id: number;
  enterpriseId: string;
  fullName: string;
  dateOfBirth: string;
  gender: string;
  nationalId: string | null;
  healthInsuranceNo: string | null;
  phoneNumber: string | null;
  address: string | null;
  status: string;
  totalLinkedPatients?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface LinkedIdentifier {
  id: number;
  identifierType: string;
  identifierValue: string;
  issuedDate: string | null;
  expiryDate: string | null;
  issuingAuthority: string | null;
  isPrimary: boolean;
}

export interface SourceRecord {
  id: number;
  localPatientCode: string;
  fullName: string;
  dateOfBirth: string;
  gender: string;
  nationalId: string | null;
  healthInsuranceNo: string | null;
  phoneNumber: string | null;
  address: string | null;
  sourceSystemId: number;
  sourceSystemName: string;
  matchStatus: string;
  matchScore: number | null;
  createdAt: string;
}

export interface MergeHistoryLog {
  id: number;
  mergeDate: string;
  mergedFromId: number;
  mergedFromEnterpriseId: string;
  mergedToId: number;
  mergedToEnterpriseId: string;
  mergedBy: string;
  reason: string;
  recordsAffected: number;
}
