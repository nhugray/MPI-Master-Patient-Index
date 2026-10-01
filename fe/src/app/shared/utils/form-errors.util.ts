export function getRequiredErrorMessage(fieldLabel: string): string {
  return `${fieldLabel} không được để trống`;
}

export const REQUIRED_ERROR_MESSAGE = 'Trường này là bắt buộc';
