import { Component, OnInit, inject, signal, computed, DestroyRef, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { PatientMasterService } from '../../services/patient-master.service';
import { PatientMaster } from '../../models/patient-master.model';
import { SourceRecord, PatientMasterHistory } from '../../models/patient-master-detail.model';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { PatientMasterEditModalComponent } from '../../components/patient-master-form/patient-master-form.component';
import { PatientFormComponent } from '../../../patient/components/patient-form/patient-form.component';
import { Patient } from '../../../patient/models/patient.model';
import { getInitials, maskNationalId, maskPhoneNumber } from '../../../../shared/utils/string.util';
import { calculateAge, formatDate, formatDateTime } from '../../../../shared/utils/date.util';
import { GENDER_OPTIONS, getGenderLabel, Gender } from '../../../../shared/enums';
import { isSuccess } from '../../../../shared/models';

@Component({
  selector: 'app-patient-master-detail-page',
  standalone: true,
  imports: [CommonModule, PatientMasterEditModalComponent, PatientFormComponent],
  templateUrl: './patient-master-detail-page.component.html',
  styleUrls: ['./patient-master-detail-page.component.css']
})
export class PatientMasterDetailPageComponent implements OnInit {
  @ViewChild('patientForm') patientForm!: PatientFormComponent;
  editingPatient = signal<PatientMaster | null>(null);

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly patientMasterService = inject(PatientMasterService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  patientMaster = signal<PatientMaster | null>(null);
  sourceRecords = signal<SourceRecord[]>([]);
  history = signal<PatientMasterHistory[]>([]);

  confidence = computed(() => {
    const records = this.sourceRecords();
    const scores = records
      .map(r => r.matchScore)
      .filter((s): s is number => s !== null && s !== undefined);
    if (scores.length > 0) {
      const min = Math.min(...scores);
      return { text: `${Math.round(min)}%`, verified: min >= 85 };
    }
    if (records.some(r => r.matchStatus === 'NEW_MASTER')) {
      return { text: 'Hồ sơ mới', verified: false };
    }
    return { text: '—', verified: false };
  });

  isLoading = signal(true);
  isLoadingSourceRecords = signal(false);
  isLoadingHistory = signal(false);

  masterId = signal<number | null>(null);

  ngOnInit(): void {
    this.route.params.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      const id = +params['id'];
      if (id) {
        this.masterId.set(id);
        this.loadPatientMaster(id);
        this.loadSourceRecords(id);
        this.loadHistory(id);
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

  loadHistory(masterId: number): void {
    this.isLoadingHistory.set(true);
    this.patientMasterService.getHistory(masterId).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (response) => {
        if (isSuccess(response)) {
          this.history.set(response.data);
        }
        this.isLoadingHistory.set(false);
      },
      error: (error) => {
        console.error('Error loading history:', error);
        this.isLoadingHistory.set(false);
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/patient-masters']);
  }

  getInitials = getInitials;
  formatDate = formatDate;
  formatDateTime = formatDateTime;

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
      'INACTIVE': 'Tạm khóa',
      'MERGED': 'Đã gộp'
    };
    return map[status] || status;
  }

  getStatusClass(status: string): string {
    const map: Record<string, string> = {
      'ACTIVE': 'status-active',
      'INACTIVE': 'status-inactive',
      'MERGED': 'status-merged'
    };
    return map[status] || 'status-active';
  }

  openEditModal(): void {
    const current = this.patientMaster();
    if (current) {
      this.editingPatient.set({ ...current });
    }
  }

  closeEditModal(): void {
    this.editingPatient.set(null);
  }

  onPatientUpdated(updated: PatientMaster): void {
    this.patientMaster.set(updated);
    this.closeEditModal();
    this.toastService.success('Thành công', 'Đã cập nhật hồ sơ gốc thành công');
    const id = this.masterId();
    if (id) {
      this.loadHistory(id);
    }
  }
  openEditPatient(record: SourceRecord): void {
    const patient: Patient = {
      id: record.id,
      sourceSystemId: record.sourceSystemId,
      sourceSystemName: record.sourceSystemName,
      localPatientCode: record.localPatientCode,
      fullName: record.fullName,
      dateOfBirth: record.dateOfBirth,
      gender: record.gender as any,
      nationalId: record.nationalId,
      phoneNumber: record.phoneNumber,
      address: record.address,
      healthInsuranceNo: record.healthInsuranceNo,
      masterPatientId: this.masterId(),
      matchStatus: record.matchStatus as any,
      matchScore: record.matchScore,
      createdAt: record.createdAt
    };
    this.patientForm.open(patient);
  }

  onPatientRecordSaved(): void {
    const id = this.masterId();
    if (id) {
      this.loadPatientMaster(id);
      this.loadSourceRecords(id);
      this.loadHistory(id);
    }
  }
}
