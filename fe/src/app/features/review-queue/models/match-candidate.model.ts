import { Gender } from '../../../shared/enums';
import { PatientMasterStatusEnum } from '../../patient-master/models/patient-master.constants';

export enum MatchDecisionEnum {
  PENDING = 'PENDING',
  AUTO_APPROVED = 'AUTO_APPROVED',
  MANUAL_APPROVED = 'MANUAL_APPROVED',
  REJECTED = 'REJECTED'
}

export interface CandidatePatientInfo {
  id: number;
  sourceSystemId: number;
  sourceSystemName: string;
  localPatientCode: string;
  fullName: string;
  dateOfBirth: string;
  gender: Gender;
  nationalId?: string;
  phoneNumber?: string;
  address?: string;
  healthInsuranceNo?: string;
  matchStatus: string;
}

export interface CandidateMasterInfo {
  id: number;
  enterpriseId: string;
  fullName: string;
  dateOfBirth: string;
  gender: Gender;
  nationalId?: string;
  healthInsuranceNo?: string;
  phoneNumber?: string;
  address?: string;
  status: PatientMasterStatusEnum;
  linkedPatientsCount?: number;
  createdAt: string;
  updatedAt: string;
}

export interface MatchCandidate {
  id: number;
  patient: CandidatePatientInfo;
  candidateMaster: CandidateMasterInfo;
  matchScore: number;
  scoreBreakdown?: string;
  weightVersion: string;
  decision: MatchDecisionEnum;
  reviewedBy?: number;
  reviewedByName?: string;
  reviewedAt?: string;
  createdAt: string;
}

export interface ReviewQueueSearchRequest {
  decision?: MatchDecisionEnum;
  page: number;
  size: number;
}

export const defaultReviewQueueSearchParams: ReviewQueueSearchRequest = {
  page: 0,
  size: 10
};

export interface ReviewDecisionRequest {
  decision: MatchDecisionEnum;
  reviewNote?: string;
  reviewerId: number;
}

export interface ScoreBreakdownItem {
  field: string;
  fieldLabel: string;
  score: number;
  maxScore: number;
}
