import { Component, EventEmitter, Input, Output, OnInit, inject, DestroyRef } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { PatientService } from '../../services/patient.service';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import {
  Patient,
  Gender,
  PatientStatus,
  CreatePatientRequest,
  UpdatePatientRequest
} from '../../models/patient.model';
import { GENDER_OPTIONS, STATUS_OPTIONS } from '../../models/patient.constants';

@Component({
  selector: 'app-patient-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './patient-form.component.html',
  styleUrls: ['./patient-form.component.css']
})
export class PatientFormComponent implements OnInit {
  @Input() patient: Patient | null = null;
  @Output() saved = new EventEmitter<Patient>();
  @Output() closed = new EventEmitter<void>();

  private readonly fb = inject(FormBuilder);
  private readonly patientService = inject(PatientService);
  private readonly toastService = inject(ToastService);
  private readonly document = inject(DOCUMENT);
  private readonly destroyRef = inject(DestroyRef);

  form!: FormGroup;
  isSubmitting = false;
  isOpen = false;

  get isEditMode(): boolean {
    return !!this.patient;
  }

  genderOptions = GENDER_OPTIONS;
  statusOptions = STATUS_OPTIONS;

  ngOnInit(): void {
    this.initForm();
  }

  private initForm(): void {
    const patient = this.patient;

    this.form = this.fb.group({
      fullName: [patient?.fullName || '', Validators.required],
      dateOfBirth: [patient?.dateOfBirth || ''],
      gender: [patient?.gender || Gender.MALE],
      nationalId: [patient?.nationalId || '', [Validators.required, Validators.pattern(/^\d{12}$/)]],
      healthInsuranceNo: [patient?.healthInsuranceNo || ''],
      phoneNumber: [patient?.phoneNumber || '', Validators.pattern(/^0\d{9}$/)],
      status: [patient?.status || PatientStatus.ACTIVE]
    });
  }

  open(patient?: Patient): void {
    this.patient = patient || null;
    this.isOpen = true;
    this.document.body.classList.add('modal-open');

    if (patient) {
      this.form.patchValue({
        fullName: patient.fullName,
        dateOfBirth: patient.dateOfBirth,
        gender: patient.gender,
        nationalId: patient.nationalId,
        healthInsuranceNo: patient.healthInsuranceNo,
        phoneNumber: patient.phoneNumber,
        status: patient.status
      });
    } else {
      this.form.reset({
        fullName: '',
        dateOfBirth: '',
        gender: Gender.MALE,
        nationalId: '',
        healthInsuranceNo: '',
        phoneNumber: '',
        status: PatientStatus.ACTIVE
      });
    }
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
      const request: UpdatePatientRequest = {
        id: this.patient!.id,
        fullName: formValue.fullName,
        dateOfBirth: formValue.dateOfBirth || null,
        gender: formValue.gender,
        nationalId: formValue.nationalId,
        healthInsuranceNo: formValue.healthInsuranceNo || null,
        phoneNumber: formValue.phoneNumber || '',
        status: formValue.status
      };

      this.patientService.update(request).pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe({
        next: (response) => this.handleSuccess(response),
        error: (error) => this.handleError(error)
      });
    } else {
      const request: CreatePatientRequest = {
        fullName: formValue.fullName,
        dateOfBirth: formValue.dateOfBirth || null,
        gender: formValue.gender,
        nationalId: formValue.nationalId,
        healthInsuranceNo: formValue.healthInsuranceNo || null,
        phoneNumber: formValue.phoneNumber || '',
        status: formValue.status
      };

      this.patientService.create(request).pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe({
        next: (response) => this.handleSuccess(response),
        error: (error) => this.handleError(error)
      });
    }
  }

  private handleSuccess(response: { statusCode: number; data: Patient }): void {
    if (response.statusCode === 200 || response.statusCode === 201) {
      this.toastService.success(
        'Thành công',
        this.isEditMode ? 'Cập nhật thông tin bệnh nhân thành công' : 'Thêm mới bệnh nhân thành công'
      );
      this.saved.emit(response.data);
      this.close();
    }
    this.isSubmitting = false;
  }

  private handleError(error: { error?: { message?: string } }): void {
    this.toastService.error(
      'Lỗi hệ thống',
      error?.error?.message || 'Đã xảy ra lỗi. Vui lòng thử lại.'
    );
    this.isSubmitting = false;
  }
}