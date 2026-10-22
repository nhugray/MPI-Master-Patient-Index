import { Component, ViewChild, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { UserListComponent } from '../../components/user-list/user-list.component';
import { UserFormComponent } from '../../components/user-form/user-form.component';
import { ConfirmDialogComponent } from '../../../../shared/components/confirm-dialog/confirm-dialog.component';
import { UserService } from '../../services/user.service';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { User } from '../../models/user.model';
import { ApiResponse, isSuccess } from '../../../../shared/models';
import { getApiErrorMessage } from '../../../../shared/utils/http-error.util';

@Component({
  selector: 'app-users-page',
  standalone: true,
  imports: [
    UserListComponent,
    UserFormComponent,
    ConfirmDialogComponent
  ],
  templateUrl: './users-page.component.html',
  styleUrls: ['./users-page.component.css']
})
export class UsersPageComponent {
  @ViewChild('userList') userList!: UserListComponent;
  @ViewChild('userForm') userForm!: UserFormComponent;

  private readonly userService = inject(UserService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  isDeleteDialogOpen = false;
  deleteMessage = '';
  userToDelete: User | null = null;

  openAddModal(): void {
    this.userForm.open();
  }

  openEditModal(user: User): void {
    this.userForm.open(user);
  }

  openDeleteConfirm(user: User): void {
    this.userToDelete = user;
    this.deleteMessage = `Bạn có chắc chắn muốn xóa người dùng "${user.name}" không? Hành động này không thể hoàn tác.`;
    this.isDeleteDialogOpen = true;
  }

  onConfirmDelete(): void {
    if (!this.userToDelete) return;

    this.userService.delete(this.userToDelete.id).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (response: ApiResponse<void>) => {
        if (isSuccess(response)) {
          this.toastService.success('Thành công', 'Xóa người dùng thành công');
          this.closeDeleteDialog();
          this.userList.reload();
        }
      },
      error: (error: { error?: { message?: string } }) => {
        this.toastService.error('Lỗi hệ thống', getApiErrorMessage(error, 'Đã xảy ra lỗi khi xóa người dùng'));
        this.closeDeleteDialog();
      }
    });
  }

  onCancelDelete(): void {
    this.closeDeleteDialog();
  }

  private closeDeleteDialog(): void {
    this.isDeleteDialogOpen = false;
    this.userToDelete = null;
  }

  onUserSaved(): void {
    this.userList.reload();
  }
}
