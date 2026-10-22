import { ACTIVE_STATUS_OPTIONS } from '../../../shared/constants';
import { FacilityType } from './facility.model';

export const FACILITY_TYPE_OPTIONS = [
  { value: FacilityType.HOSPITAL, label: 'Bệnh viện' },
  { value: FacilityType.CLINIC, label: 'Phòng khám' },
  { value: FacilityType.OTHER, label: 'Khác' }
];

export const FACILITY_STATUS_OPTIONS = ACTIVE_STATUS_OPTIONS;
export const STATUS_OPTIONS = ACTIVE_STATUS_OPTIONS;

