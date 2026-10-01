import { Component, ViewChild, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { PatientListComponent } from '../components/patient-list/patient-list.component';
import { PatientFormComponent } from '../components/patient-form/patient-form.component';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { PatientService } from '../services/patient.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { Patient } from '../models/patient.model';
import { ApiResponse, isSuccess } from '../../../shared/models';
import { getApiErrorMessage } from '../../../shared/utils/http-error.util';

@Component({
  selector: 'app-patients-page',
  standalone: true,
  imports: [
    PatientListComponent,
    PatientFormComponent,
    ConfirmDialogComponent
  ],
  templateUrl: './patients-page.component.html',
  styleUrls: ['./patients-page.component.css']
})
export class PatientsPageComponent {
  @ViewChild('patientList') patientList!: PatientListComponent;
  @ViewChild('patientForm') patientForm!: PatientFormComponent;

  private readonly patientService = inject(PatientService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  isDeleteDialogOpen = false;
  deleteMessage = '';
  patientToDelete: Patient | null = null;

  openAddModal(): void {
    this.patientForm.open();
  }

  openEditModal(patient: Patient): void {
    this.patientForm.open(patient);
  }

  openDeleteConfirm(patient: Patient): void {
    this.patientToDelete = patient;
    this.deleteMessage = `Bạn có chắc chắn muốn xóa hồ sơ bệnh nhân "${patient.fullName}" không? Hành động này không thể hoàn tác và toàn bộ dữ liệu liên quan sẽ bị loại bỏ khỏi hệ thống.`;
    this.isDeleteDialogOpen = true;
  }

  onConfirmDelete(): void {
    if (!this.patientToDelete) return;

    this.patientService.delete(this.patientToDelete.id).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (response: ApiResponse<void>) => {
        if (isSuccess(response)) {
          this.toastService.success('Thành công', 'Xóa bệnh nhân thành công');
          this.closeDeleteDialog();
          this.patientList.reload();
        }
      },
      error: (error: { error?: { message?: string } }) => {
        this.toastService.error('Lỗi hệ thống', getApiErrorMessage(error, 'Đã xảy ra lỗi khi xóa bệnh nhân'));
        this.closeDeleteDialog();
      }
    });
  }

  onCancelDelete(): void {
    this.closeDeleteDialog();
  }

  private closeDeleteDialog(): void {
    this.isDeleteDialogOpen = false;
    this.patientToDelete = null;
  }

  onPatientSaved(): void {
    this.patientList.reload();
  }
}