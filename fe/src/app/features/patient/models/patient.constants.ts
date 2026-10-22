import { MatchStatus } from '../../../shared/enums';
import { PatientStatus } from './patient.model';

export const MATCH_STATUS_OPTIONS = [
  { value: MatchStatus.PENDING, label: 'Chờ đối chiếu' },
  { value: MatchStatus.MATCHED, label: 'Đã đối chiếu' },
  { value: MatchStatus.NEW_MASTER, label: 'Tạo master mới' },
  { value: MatchStatus.REJECTED, label: 'Từ chối' }
];

export const STATUS_OPTIONS = [
  { value: PatientStatus.ACTIVE, label: 'Đang điều trị' },
  { value: PatientStatus.MERGED, label: 'Đã gộp' },
  { value: PatientStatus.DECEASED, label: 'Đã mất' },
  { value: PatientStatus.INACTIVE, label: 'Không hoạt động' }
];
