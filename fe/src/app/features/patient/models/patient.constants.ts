import { Gender } from '../../../shared/enums';
import { PatientStatus } from './patient.model';

export const GENDER_OPTIONS = [
  { value: Gender.MALE, label: 'Nam' },
  { value: Gender.FEMALE, label: 'Nữ' },
  { value: Gender.OTHER, label: 'Khác' },
  { value: Gender.UNKNOWN, label: 'Chưa rõ' }
];

export const STATUS_OPTIONS = [
  { value: PatientStatus.ACTIVE, label: 'Đang điều trị' },
  { value: PatientStatus.MERGED, label: 'Đã gộp' },
  { value: PatientStatus.DECEASED, label: 'Đã mất' },
  { value: PatientStatus.INACTIVE, label: 'Không hoạt động' }
];
