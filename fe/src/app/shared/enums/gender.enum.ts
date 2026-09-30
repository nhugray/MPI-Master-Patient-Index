export enum Gender {
  MALE = 'MALE',
  FEMALE = 'FEMALE',
  OTHER = 'OTHER'
}

export interface GenderOption {
  value: Gender;
  label: string;
}

export const GENDER_OPTIONS: GenderOption[] = [
  { value: Gender.MALE, label: 'Nam' },
  { value: Gender.FEMALE, label: 'Nữ' },
  { value: Gender.OTHER, label: 'Khác' }
];

// export 1 hàm 
export const getGenderLabel = (gender: Gender): string => {
  switch (gender) {
    case Gender.MALE: return 'male';
    case Gender.FEMALE: return 'female';
    case Gender.OTHER: return 'other';
  }
};
export const GENDER_ALL_OPTION = { value: 'ALL', label: 'Tất cả' };