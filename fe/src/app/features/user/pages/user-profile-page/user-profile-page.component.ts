import { Component, OnInit, inject, signal, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { AuthService } from '../../../auth/services/auth.service';
import { UserService } from '../../services/user.service';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { TokenStorageService } from '../../../../core/services/token-storage.service';
import { AuthUser } from '../../../auth/models/auth-user.model';
import { UserStatus } from '../../../../shared/enums';
import { emailValidator } from '../../../../shared/validators/email.validator';
import { getInitials } from '../../../../shared/utils/string.util';
import { formatDateTime } from '../../../../shared/utils/date.util';
import { isSuccess } from '../../../../shared/models';
import { getApiErrorMessage } from '../../../../shared/utils/http-error.util';
import { UserChangePasswordComponent } from './user-change-password/user-change-password.component';

@Component({
  selector: 'app-user-profile-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, UserChangePasswordComponent],
  templateUrl: './user-profile-page.component.html',
  styleUrls: ['./user-profile-page.component.css']
})
export class UserProfilePageComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly userService = inject(UserService);
  private readonly toastService = inject(ToastService);
  private readonly tokenStorage = inject(TokenStorageService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  user = signal<AuthUser | null>(null);
  isLoading = signal(true);
  isEditing = signal(false);
  isSubmitting = signal(false);
  showChangePassword = signal(false);

  form!: FormGroup;

  readonly getInitials = getInitials;
  readonly formatDateTime = formatDateTime;

  ngOnInit(): void {
    this.loadProfile();
  }

  private loadProfile(): void {
    this.isLoading.set(true);
    this.authService.getMe().pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (response) => {
        if (isSuccess(response)) {
          this.user.set(response.data);
          this.initForm(response.data);
        }
        this.isLoading.set(false);
      },
      error: () => {
        this.toastService.error('Lỗi', 'Không thể tải thông tin tài khoản');
        this.isLoading.set(false);
      }
    });
  }

  private initForm(user: AuthUser): void {
    this.form = this.fb.group({
      name: [user.name, [Validators.required]],
      email: [user.email, [Validators.required, emailValidator()]],
      address: [user.address || '']
    });
  }

  openEdit(): void {
    this.isEditing.set(true);
  }

  cancelEdit(): void {
    const u = this.user();
    if (u) this.initForm(u);
    this.isEditing.set(false);
  }

  onSubmit(): void {
    if (this.form.invalid || this.isSubmitting()) {
      this.form.markAllAsTouched();
      return;
    }
    const u = this.user();
    if (!u) return;

    this.isSubmitting.set(true);
    const { name, email, address } = this.form.value;

    this.userService.update({
      id: u.id,
      name,
      email,
      password: undefined,
      address: address || null,
      status: (u.status as UserStatus) || UserStatus.ACTIVE,
      roleIds: u.roles.map((r: any) => r.id)
    }).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (response) => {
        if (isSuccess(response)) {
          const updated: AuthUser = {
            ...u,
            name: response.data.name,
            email: response.data.email,
            address: response.data.address || undefined
          };
          this.tokenStorage.setCurrentUser(updated);
          this.user.set(updated);
          this.toastService.success('Thành công', 'Cập nhật thông tin thành công');
          this.isEditing.set(false);
        }
        this.isSubmitting.set(false);
      },
      error: (error) => {
        this.toastService.error('Lỗi', getApiErrorMessage(error, 'Không thể cập nhật thông tin'));
        this.isSubmitting.set(false);
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/users']);
  }

  getStatusLabel(status: string | undefined): string {
    const map: Record<string, string> = {
      'ACTIVE': 'Đang hoạt động',
      'INACTIVE': 'Không hoạt động',
      'SUSPENDED': 'Bị tạm khóa'
    };
    return map[status ?? ''] ?? (status || 'Hoạt động');
  }

  getStatusClass(status: string | undefined): string {
    const map: Record<string, string> = {
      'ACTIVE': 'status-active',
      'INACTIVE': 'status-inactive',
      'SUSPENDED': 'status-suspended'
    };
    return map[status ?? ''] ?? 'status-active';
  }

  getRoleIcon(roleName: string): string {
    const map: Record<string, string> = {
      'ADMIN': 'admin_panel_settings',
      'REVIEWER': 'rule_settings',
      'VIEWER': 'visibility',
      'USER': 'person'
    };
    return map[roleName?.toUpperCase()] ?? 'badge';
  }

  getRoleDescription(roleName: string): string {
    const map: Record<string, string> = {
      'ADMIN': 'Toàn quyền cấu hình quản trị hệ thống, danh mục, phân quyền tài khoản & giám sát an ninh dữ liệu.',
      'REVIEWER': 'Kiểm tra, phê duyệt gộp / tách hồ sơ bệnh nhân nghi ngờ trùng lặp và đối soát thuật toán.',
      'VIEWER': 'Tra cứu và theo dõi hồ sơ định danh bệnh nhân (Golden Record), chỉ xem không chỉnh sửa.',
      'USER': 'Thao tác nạp dữ liệu lâm sàng, kích hoạt tìm kiếm và so khớp bệnh nhân theo thẩm quyền.'
    };
    return map[roleName?.toUpperCase()] ?? 'Vai trò chuyên môn trong hệ thống cơ sở dữ liệu định danh bệnh nhân.';
  }

  copyUserId(id: number): void {
    const code = `USR-${('00000' + id).slice(-5)}`;
    navigator.clipboard?.writeText(code);
    this.toastService.success('Đã sao chép', `Mã định danh ${code} đã được lưu vào bộ nhớ tạm`);
  }
}
