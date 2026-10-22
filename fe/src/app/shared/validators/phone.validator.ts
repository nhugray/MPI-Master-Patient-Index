import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export function phoneValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) {
      return null;
    }

    const phonePattern = /^0\d{9}$/;
    const valid = phonePattern.test(control.value);

    return valid ? null : { invalidPhone: { value: control.value } };
  };
}

export { formatPhoneNumber } from '../utils/string.util';


export const PHONE_ERROR_MESSAGE = 'Số điện thoại không hợp lệ';

export const PHONE_PATTERN = /^0\d{9}$/;
