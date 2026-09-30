import { Component, OnInit, inject, signal, computed, DestroyRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { PatientMasterService } from '../../services/patient-master.service';
import { PatientMaster } from '../../models/patient-master.model';
import { SourceRecord } from '../../models/patient-master-detail.model';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { PatientMasterEditModalComponent } from '../../components/patient-master-form/patient-master-form.component';

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
        if (response.statusCode === 200) {
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
        if (response.statusCode === 200) {
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

  getInitials(fullName: string): string {
    if (!fullName) return '?';
    const parts = fullName.trim().split(' ');
    if (parts.length >= 2) {
      return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }
    return fullName[0]?.toUpperCase() || '?';
  }

  getAge(dateOfBirth: string): number {
    if (!dateOfBirth) return 0;
    const today = new Date();
    const birthDate = new Date(dateOfBirth);
    let age = today.getFullYear() - birthDate.getFullYear();
    const monthDiff = today.getMonth() - birthDate.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
      age--;
    }
    return age;
  }

  getGenderDisplay(gender: string): string {
    const map: Record<string, string> = {
      'MALE': 'Nam',
      'FEMALE': 'Nữ',
      'OTHER': 'Khác'
    };
    return map[gender] || gender;
  }

  getGenderIcon(gender: string): string {
    const map: Record<string, string> = {
      'MALE': 'male',
      'FEMALE': 'female',
      'OTHER': 'transgender'
    };
    return map[gender] || 'help';
  }

  maskNationalId(nationalId: string | null | undefined): string {
    if (!nationalId) return '-';
    if (nationalId.length <= 4) return nationalId;
    return nationalId.substring(0, 3) + 'x'.repeat(nationalId.length - 7) + nationalId.substring(nationalId.length - 4);
  }

  maskPhoneNumber(phoneNumber: string | null | undefined): string {
    if (!phoneNumber) return '-';
    if (phoneNumber.length <= 4) return phoneNumber;
    return phoneNumber.substring(0, 3) + 'x'.repeat(phoneNumber.length - 6) + phoneNumber.substring(phoneNumber.length - 3);
  }

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

  formatDate(dateString: string): string {
    if (!dateString) return '-';
    const date = new Date(dateString);
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    return `${day}/${month}/${year}`;
  }

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
