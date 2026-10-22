import { Component, EventEmitter, inject, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { emailValidator } from '../../../../shared/validators/email.validator';
import { passwordValidator } from '../../../../shared/validators/password.validator';
import { LoginCredentials } from '../../models/login-request.model';

@Component({
  selector: 'app-login-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login-form.component.html',
  styleUrls: ['./login-form.component.css']
})
export class LoginFormComponent implements OnInit {
  @Output() submitLogin = new EventEmitter<LoginCredentials>();

  private fb = inject(FormBuilder);

  loginForm!: FormGroup;
  isSubmitting = false;
  showPassword = false;

  ngOnInit(): void {
    this.initializeForm();
  }

  private initializeForm(): void {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, emailValidator()]],
      password: ['', [Validators.required, passwordValidator()]],
      rememberMe: [false]
    });

  }

  onSubmit(): void {
    if (this.loginForm.invalid || this.isSubmitting) {
      return;
    }

    this.isSubmitting = true;
    const credentials: LoginCredentials = this.loginForm.getRawValue();
    this.submitLogin.emit(credentials);

    setTimeout(() => {
      this.isSubmitting = false;
    }, 500);
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.loginForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  getErrorMessage(fieldName: string): string {
    const field = this.loginForm.get(fieldName);

    if (!field || !field.errors) {
      return '';
    }

    if (field.hasError('required')) {
      if (fieldName === 'email') {
        return 'Vui lòng nhập email đăng nhập';
      }
      return 'Vui lòng nhập mật khẩu';
    }

    if (field.hasError('invalidEmail')) {
      return field.errors['invalidEmail'].message;
    }

    if (field.hasError('passwordInvalid')) {
      const passwordErrors = field.errors['passwordInvalid'];
      if (passwordErrors.minLength) {
        return passwordErrors.minLength.message;
      }
      if (passwordErrors.maxLength) {
        return passwordErrors.maxLength.message;
      }
    }

    return 'Dữ liệu không hợp lệ';
  }

  reset(): void {
    this.loginForm.reset({ rememberMe: false });
    this.showPassword = false;
    this.isSubmitting = false;
  }
}
