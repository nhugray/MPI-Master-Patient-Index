export interface WeightConfig {
  id: number;
  version: string;
  nameWeight: number;
  dobWeight: number;
  nationalIdWeight: number;
  healthInsuranceWeight: number;
  phoneWeight: number;
  addressWeight: number;
  autoApprovalThreshold: number;
  manualReviewThreshold: number;
  isActive: boolean;
  description: string | null;
  createdBy: string | null;
  createdAt: string;
  deployedAt: string | null;
}

export interface CreateWeightConfigRequest {
  nameWeight: number;
  dobWeight: number;
  nationalIdWeight: number;
  healthInsuranceWeight: number;
  phoneWeight: number;
  addressWeight: number;
  autoApprovalThreshold?: number;
  manualReviewThreshold?: number;
  description: string | null;
  createdBy: string | null;
}

export type WeightFieldKey =
  | 'nameWeight'
  | 'dobWeight'
  | 'nationalIdWeight'
  | 'healthInsuranceWeight'
  | 'phoneWeight'
  | 'addressWeight';
