import { Component, OnInit, inject, signal, computed, DestroyRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { PatientMasterService } from '../../services/patient-master.service';
import { PatientMaster } from '../../models/patient-master.model';
import { SourceRecord } from '../../models/patient-master-detail.model';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { PatientMasterEditModalComponent } from '../../components/patient-master-form/patient-master-form.component';
import { getInitials, maskNationalId, maskPhoneNumber } from '../../../../shared/utils/string.util';
import { calculateAge, formatDate } from '../../../../shared/utils/date.util';
import { GENDER_OPTIONS, getGenderLabel, Gender } from '../../../../shared/enums';
import { isSuccess } from '../../../../shared/models';

@Component({
  selector: 'app-patient-master-detail-page',
  standalone: true,
  imports: [CommonModule, PatientMasterEditModalComponent],
  templateUrl: './patient-master-detail-page.component.html',
  styleUrls: ['./patient-master-detail-page.component.css']
})
export class PatientMasterDetailPageComponent implements OnInit {
  editingPatient = signal<PatientMaster | null>(null);

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly patientMasterService = inject(PatientMasterService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  patientMaster = signal<PatientMaster | null>(null);
  sourceRecords = signal<SourceRecord[]>([]);
  isLoading = signal(true);
  isLoadingSourceRecords = signal(false);

  masterId = signal<number | null>(null);


  mockHistory = [
    {
      action: 'Cập nhật thủ công (Manual Edit)',
      date: 'Hôm nay, 14:30',
      by: 'Admin Trần Văn B',
      detail: 'Cập nhật địa chỉ thường trú theo CCCD mới cung cấp.',
      color: 'brand-primary'
    },
    {
      action: 'Gộp thủ công (Manual Merge)',
      date: '15/10/2023, 09:15',
      by: 'Reviewer Lê Thị C',
      detail: 'Gộp hồ sơ LAB-2023-112 từ LIS-LAB. Lý do: Thiếu dấu tiếng Việt nhưng khớp SĐT và Ngày sinh.',
      color: 'warning-high'
    },
    {
      action: 'Tự động gộp (Auto Merge)',
      date: '10/10/2023, 11:20',
      by: 'Hệ thống (Rule Engine)',
      detail: 'Tạo hồ sơ gốc và gộp hồ sơ REC-8891-A từ HIS-CORE. Điểm tin cậy: 98%.',
      color: 'success-optimal'
    }
  ];

  ngOnInit(): void {
    this.route.params.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      const id = +params['id'];
      if (id) {
        this.masterId.set(id);
        this.loadPatientMaster(id);
        this.loadSourceRecords(id);
      }
    });
  }

  loadPatientMaster(id: number): void {
    this.isLoading.set(true);
    this.patientMasterService.getById(id).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (response) => {
        if (isSuccess(response)) {
          this.patientMaster.set(response.data);
        }
        this.isLoading.set(false);
      },
      error: (error) => {
        this.toastService.error('Lỗi', 'Không thể tải thông tin hồ sơ gốc');
        this.isLoading.set(false);
      }
    });
  }

  loadSourceRecords(masterId: number): void {
    this.isLoadingSourceRecords.set(true);
    this.patientMasterService.getLinkedPatients(masterId, 0, 10).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (response) => {
        if (isSuccess(response)) {
          this.sourceRecords.set(response.data.result);
        }
        this.isLoadingSourceRecords.set(false);
      },
      error: (error) => {
        console.error('Error loading source records:', error);
        this.isLoadingSourceRecords.set(false);
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/patient-masters']);
  }

  getInitials = getInitials;

  getAge(dateOfBirth: string): number {
    return calculateAge(dateOfBirth);
  }

  getGenderDisplay(gender: string): string {
    const option = GENDER_OPTIONS.find(o => o.value === gender);
    return option ? option.label : gender;
  }

  getGenderIcon = getGenderLabel;

  maskNationalId = maskNationalId;

  maskPhoneNumber = maskPhoneNumber;

  getStatusDisplay(status: string): string {
    const map: Record<string, string> = {
      'ACTIVE': 'Hoạt động',
      'MERGED': 'Đã gộp',
      'INACTIVE': 'Không hoạt động'
    };
    return map[status] || status;
  }

  getMatchStatusBadgeClass(matchStatus: string): string {
    const map: Record<string, string> = {
      'AUTO_MERGED': 'status-auto-merged',
      'MANUAL_MERGED': 'status-manual-merged',
      'PENDING': 'status-pending'
    };
    return map[matchStatus] || '';
  }

  getMatchStatusDisplay(matchStatus: string): string {
    const map: Record<string, string> = {
      'AUTO_MERGED': 'Auto-merged',
      'MANUAL_MERGED': 'Manual-merged',
      'PENDING': 'Pending'
    };
    return map[matchStatus] || matchStatus;
  }

  formatDate = formatDate;

  openEditModal(): void {
    const patient = this.patientMaster();
    if (patient) {
      this.editingPatient.set(patient);
    }
  }

  closeEditModal(): void {
    this.editingPatient.set(null);
  }

  onPatientUpdated(updatedPatient: PatientMaster): void {
    this.patientMaster.set(updatedPatient);
    this.toastService.success('Thành công', 'Cập nhật hồ sơ gốc thành công');
    this.editingPatient.set(null);
  }

}
