import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
export function passwordValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) {
      return null;
    }

    const password = control.value;
    const errors: any = {};

    if (password.length < 6) {
      errors.minLength = {
        message: 'Mật khẩu phải có ít nhất 6 ký tự',
        requiredLength: 8,
        actualLength: password.length
      };
    }

    if (password.length > 128) {
      errors.maxLength = {
        message: 'Mật khẩu không được quá 128 ký tự',
        maxLength: 128,
        actualLength: password.length
      };
    }

    return Object.keys(errors).length > 0 ? { passwordInvalid: errors } : null;
  };
}

export function strongPasswordValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) {
      return null;
    }

    const password = control.value;
    const errors: any = {};

    if (password.length < 6) {
      errors.minLength = 'Mật khẩu phải có ít nhất 6 ký tự';
    }

    if (!/[A-Z]/.test(password)) {
      errors.uppercase = 'Mật khẩu phải chứa ít nhất 1 chữ hoa';
    }

    if (!/[a-z]/.test(password)) {
      errors.lowercase = 'Mật khẩu phải chứa ít nhất 1 chữ thường';
    }

    if (!/\d/.test(password)) {
      errors.number = 'Mật khẩu phải chứa ít nhất 1 số';
    }

    if (!/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password)) {
      errors.specialChar = 'Mật khẩu phải chứa ít nhất 1 ký tự đặc biệt';
    }

    return Object.keys(errors).length > 0 ? { strongPassword: errors } : null;
  };
}

export function confirmPasswordValidator(passwordField: string): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value || !control.parent) {
      return null;
    }

    const password = control.parent.get(passwordField)?.value;
    const confirmPassword = control.value;

    if (password !== confirmPassword) {
      return {
        passwordMismatch: {
          message: 'Mật khẩu xác nhận không khớp'
        }
      };
    }

    return null;
  };
}
