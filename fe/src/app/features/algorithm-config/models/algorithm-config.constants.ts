import { WeightFieldKey } from './algorithm-config.model';

export interface WeightFieldDefinition {
  key: WeightFieldKey;
  label: string;
  description: string;
  colorClass: string;
}

export const WEIGHT_FIELDS: WeightFieldDefinition[] = [
  {
    key: 'nameWeight',
    label: 'Tên Pháp lý (Given + Surname)',
    // mô tả tiếng việt
    description: 'Trọng số cho tên pháp lý, bao gồm tên và họ.',
    colorClass: 'text-primary'
  },
  {
    key: 'dobWeight',
    label: 'Ngày sinh (DOB)',
    // mô tả tiếng việt
    description: 'Trọng số cho ngày sinh, bao gồm ngày tháng năm sinh.',
    colorClass: 'text-primary'
  },
  {
    key: 'nationalIdWeight',
    label: 'Số CCCD/Định danh (CCCD/SSN)',
    description: 'Trọng số cho số căn cước công dân hoặc số định danh, hỗ trợ xác thực chéo.',
    colorClass: 'text-error'
  },
  {
    key: 'phoneWeight',
    label: 'Số Điện thoại',
    description: 'Trọng số cho số điện thoại, hỗ trợ xác thực chéo.',
    colorClass: 'text-secondary'
  },
  {
    key: 'addressWeight',
    label: 'Địa chỉ (Khớp mờ)',
    description: 'Độ tin cậy thấp do dữ liệu thường không đồng nhất.',
    colorClass: 'text-secondary'
  }
];

export const TOTAL_WEIGHT_EXPECTED = 100;

export const DEFAULT_WEIGHT_FORM: Record<WeightFieldKey, number> = {
  nameWeight: 40,
  dobWeight: 25,
  nationalIdWeight: 30,
  phoneWeight: 3,
  addressWeight: 2
};

export const DEFAULT_AUTO_APPROVAL_THRESHOLD = 85;
export const DEFAULT_MANUAL_REVIEW_THRESHOLD = 50;
