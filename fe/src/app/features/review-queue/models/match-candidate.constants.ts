import { MatchDecisionEnum } from './match-candidate.model';

export interface MatchDecisionOption {
  value: MatchDecisionEnum | 'ALL';
  label: string;
  color?: string;
}

export const MATCH_DECISION_OPTIONS: MatchDecisionOption[] = [
  { value: 'ALL', label: 'Tất cả', color: 'primary' },
  { value: MatchDecisionEnum.PENDING, label: 'Đang chờ', color: 'warning' },
  { value: MatchDecisionEnum.AUTO_APPROVED, label: 'Tự động duyệt', color: 'success' },
  { value: MatchDecisionEnum.MANUAL_APPROVED, label: 'Duyệt thủ công', color: 'info' },
  { value: MatchDecisionEnum.REJECTED, label: 'Đã từ chối', color: 'error' }
];

export const SCORE_LEVEL = {
  HIGH: { min: 85, label: 'Rất cao', color: 'error' },
  MEDIUM: { min: 70, label: 'Cao', color: 'warning' },
  LOW: { min: 50, label: 'Trung bình', color: 'primary' }
};

export function getScoreLevel(score: number): { label: string; color: string } {
  if (score >= SCORE_LEVEL.HIGH.min) return { label: SCORE_LEVEL.HIGH.label, color: SCORE_LEVEL.HIGH.color };
  if (score >= SCORE_LEVEL.MEDIUM.min) return { label: SCORE_LEVEL.MEDIUM.label, color: SCORE_LEVEL.MEDIUM.color };
  return { label: SCORE_LEVEL.LOW.label, color: SCORE_LEVEL.LOW.color };
}

export function getDecisionLabel(decision: MatchDecisionEnum): string {
  const option = MATCH_DECISION_OPTIONS.find(o => o.value === decision);
  return option ? option.label : decision;
}

export function getDecisionColor(decision: MatchDecisionEnum): string {
  const option = MATCH_DECISION_OPTIONS.find(o => o.value === decision);
  return option?.color || 'default';
}
