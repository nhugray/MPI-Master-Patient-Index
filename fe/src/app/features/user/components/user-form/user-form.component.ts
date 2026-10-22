import { Component, EventEmitter, Input, Output, OnInit, inject, DestroyRef } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { UserService } from '../../services/user.service';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { User, CreateUserRequest, UpdateUserRequest } from '../../models/user.model';
import { USER_STATUS_OPTIONS } from '../../../../shared/enums';
import { emailValidator } from '../../../../shared/validators/email.validator';
import { getApiErrorMessage } from '../../../../shared/utils/http-error.util';
import { getRequiredErrorMessage } from '../../../../shared/utils/form-errors.util';
import { isSuccess } from '../../../../shared/models';

@Component({
  selector: 'app-user-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './user-form.component.html',
  styleUrls: ['./user-form.component.css']
})
export class UserFormComponent implements OnInit {
  @Input() user: User | null = null;
  @Output() saved = new EventEmitter<User>();
  @Output() closed = new EventEmitter<void>();

  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  private readonly toastService = inject(ToastService);
  private readonly document = inject(DOCUMENT);
  private readonly destroyRef = inject(DestroyRef);

  form!: FormGroup;
  isSubmitting = false;
  isOpen = false;
  roles: any[] = [];

  get isEditMode(): boolean {
    return !!this.user;
  }

  statusOptions = USER_STATUS_OPTIONS;
  readonly nameRequiredError = getRequiredErrorMessage('Tên');
  readonly emailRequiredError = getRequiredErrorMessage('Email');

  ngOnInit(): void {
    this.initForm();
    this.loadRoles();
  }

  private initForm(): void {
    const user = this.user;
    this.form = this.fb.group({
      name: [user?.name || '', Validators.required],
      email: [user?.email || '', [Validators.required, emailValidator()]],
      password: [''],
      address: [user?.address || ''],
      status: [user?.status || 'ACTIVE'],
      roleIds: [user?.roles.map(r => r.id) || []]
    });
  }

  open(user?: User): void {
    this.user = user || null;
    this.isOpen = true;
    this.document.body.classList.add('modal-open');
    this.initForm();
  }

  close(): void {
    this.isOpen = false;
    this.document.body.classList.remove('modal-open');
    this.closed.emit();
  }

  onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal-backdrop')) {
      this.close();
    }
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    const formValue = this.form.value;

    if (this.isEditMode) {
      const request: UpdateUserRequest = {
        id: this.user!.id,
        name: formValue.name,
        email: formValue.email,
        password: formValue.password || undefined,
        address: formValue.address || null,
        status: formValue.status,
        roleIds: formValue.roleIds
      };

      this.userService.update(request).pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe({
        next: (response) => this.handleSuccess(response),
        error: (error) => this.handleError(error)
      });
    } else {
      const request: CreateUserRequest = {
        name: formValue.name,
        email: formValue.email,
        password: '12345678',
        address: formValue.address || null,
        status: formValue.status,
        roleIds: formValue.roleIds
      };

      this.userService.create(request).pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe({
        next: (response) => this.handleSuccess(response),
        error: (error) => this.handleError(error)
      });
    }
  }

  private loadRoles(): void {
    this.userService.getAllRoles().pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (response) => {
        if (isSuccess(response)) {
          this.roles = response.data.result;
        }
      },
      error: () => {
        this.toastService.error('Lỗi', 'Không thể tải danh sách vai trò');
      }
    });
  }

  private handleSuccess(response: { statusCode: number; data: User }): void {
    if (isSuccess(response)) {
      this.toastService.success(
        'Thành công',
        this.isEditMode ? 'Cập nhật thông tin người dùng thành công' : 'Thêm mới người dùng thành công'
      );
      this.saved.emit(response.data);
      this.close();
    }
    this.isSubmitting = false;
  }

  private handleError(error: { error?: { message?: string } }): void {
    this.toastService.error(
      'Lỗi hệ thống',
      getApiErrorMessage(error, 'Đã xảy ra lỗi. Vui lòng thử lại.')
    );
    this.isSubmitting = false;
  }
  get roleIdsControl() {
    return this.form.get('roleIds');
  }
  onRoleChange(event: any, roleId: number | string) {
    const roleIds = this.roleIdsControl?.value || [];

    if (event.target.checked) {
      if (!roleIds.includes(roleId)) {
        const updatedRoles = [...roleIds, roleId];
        this.form.get('roleIds')?.setValue(updatedRoles);
      }
    } else {
      const updatedRoles = roleIds.filter((id: any) => id !== roleId);
      this.form.get('roleIds')?.setValue(updatedRoles);
    }
  }

}
