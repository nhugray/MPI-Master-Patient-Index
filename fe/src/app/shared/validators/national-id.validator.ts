import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export function nationalIdValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) {
      return null;
    }

    const valid = NATIONAL_ID_PATTERN.test(control.value);

    return valid ? null : { invalidNationalId: { value: control.value } };
  };
}

export const NATIONAL_ID_ERROR_MESSAGE = 'Số CCCD phải gồm 12 chữ số';

export const NATIONAL_ID_PATTERN = /^\d{12}$/;
