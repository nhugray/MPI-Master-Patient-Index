import { Component, EventEmitter, Output, inject, signal, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { AuthService } from '../../../../auth/services/auth.service';
import { ToastService } from '../../../../../shared/components/toast/toast.service';
import { getApiErrorMessage } from '../../../../../shared/utils/http-error.util';
import { isSuccess } from '../../../../../shared/models';

@Component({
  selector: 'app-user-change-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './user-change-password.component.html',
  styleUrls: ['./user-change-password.component.css']
})
export class UserChangePasswordComponent {
  @Output() closed = new EventEmitter<void>();

  private readonly authService = inject(AuthService);
  private readonly toastService = inject(ToastService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  isSubmitting = signal(false);
  showCurrent = signal(false);
  showNew = signal(false);
  showConfirm = signal(false);

  form: FormGroup = this.fb.group(
    {
      currentPassword: ['', [Validators.required]],
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]]
    },
    { validators: this.passwordsMatchValidator }
  );

  private passwordsMatchValidator(group: AbstractControl) {
    const np = group.get('newPassword')?.value;
    const cp = group.get('confirmPassword')?.value;
    if (np && cp && np !== cp) {
      group.get('confirmPassword')?.setErrors({ mismatch: true });
      return { mismatch: true };
    }
    const cpCtrl = group.get('confirmPassword');
    if (cpCtrl?.errors?.['mismatch']) {
      const { mismatch, ...rest } = cpCtrl.errors;
      cpCtrl.setErrors(Object.keys(rest).length ? rest : null);
    }
    return null;
  }

  onSubmit(): void {
    if (this.form.invalid || this.isSubmitting()) {
      this.form.markAllAsTouched();
      return;
    }

    const { currentPassword, newPassword, confirmPassword } = this.form.value;
    this.isSubmitting.set(true);

    this.authService.changePassword({ currentPassword, newPassword, confirmPassword })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          if (isSuccess(response)) {
            this.toastService.success('Thành công', 'Mật khẩu đã được thay đổi thành công');
            this.closeModal();
          }
          this.isSubmitting.set(false);
        },
        error: (err) => {
          this.toastService.error('Lỗi', getApiErrorMessage(err, 'Không thể đổi mật khẩu'));
          this.isSubmitting.set(false);
        }
      });
  }

  closeModal(): void {
    this.closed.emit();
  }

  onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('cp-modal-backdrop')) {
      this.closeModal();
    }
  }

  getStrength(password: string): 'weak' | 'medium' | 'strong' {
    if (!password || password.length < 6) return 'weak';
    const hasUpper = /[A-Z]/.test(password);
    const hasLower = /[a-z]/.test(password);
    const hasNumber = /[0-9]/.test(password);
    const hasSpecial = /[^A-Za-z0-9]/.test(password);
    const score = [hasUpper, hasLower, hasNumber, hasSpecial].filter(Boolean).length;
    if (password.length >= 10 && score >= 3) return 'strong';
    if (password.length >= 6 && score >= 2) return 'medium';
    return 'weak';
  }

  getStrengthLabel(password: string): string {
    const s = this.getStrength(password);
    return s === 'strong' ? 'Mạnh' : s === 'medium' ? 'Trung bình' : 'Yếu';
  }
}
