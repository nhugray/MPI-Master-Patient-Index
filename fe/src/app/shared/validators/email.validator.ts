import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export function emailValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) {
      return null;
    }

    const email = control.value.trim();

    const emailPattern = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

    if (!emailPattern.test(email)) {
      return {
        invalidEmail: {
          message: 'Email không hợp lệ',
          value: email
        }
      };
    }

    const commonDomains = ['gmail.com', 'yahoo.com', 'outlook.com', 'hotmail.com'];
    const domain = email.split('@')[1];

    if (email.length > 254) {
      return {
        emailTooLong: {
          message: 'Email quá dài (tối đa 254 ký tự)',
          value: email
        }
      };
    }

    return null;
  };
}

export function emailDomainValidator(allowedDomains: string[]): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) {
      return null;
    }

    const email = control.value.trim();
    const domain = email.split('@')[1];

    if (!allowedDomains.includes(domain)) {
      return {
        invalidDomain: {
          message: `Email phải từ domain: ${allowedDomains.join(', ')}`,
          allowedDomains,
          currentDomain: domain
        }
      };
    }

    return null;
  };
}
