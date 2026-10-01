import { Component, EventEmitter, Input, Output, OnInit, inject, DestroyRef } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { PatientService } from '../../services/patient.service';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { Gender, MatchStatus } from '../../../../shared/enums';
import {
  Patient,
  CreatePatientRequest,
  UpdatePatientRequest
} from '../../models/patient.model';
import { GENDER_OPTIONS } from '../../../../shared/enums';
import { MATCH_STATUS_OPTIONS } from '../../models/patient.constants';
import { phoneValidator, PHONE_ERROR_MESSAGE } from '../../../../shared/validators/phone.validator';
import { nationalIdValidator, NATIONAL_ID_ERROR_MESSAGE } from '../../../../shared/validators/national-id.validator';
import { getApiErrorMessage } from '../../../../shared/utils/http-error.util';
import { getRequiredErrorMessage } from '../../../../shared/utils/form-errors.util';
import { isSuccess } from '../../../../shared/models';

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
  matchStatusOptions = MATCH_STATUS_OPTIONS;

  readonly nationalIdErrorMessage = NATIONAL_ID_ERROR_MESSAGE;
  readonly phoneErrorMessage = PHONE_ERROR_MESSAGE;

  readonly sourceSystemIdRequiredError = getRequiredErrorMessage('ID hệ thống nguồn');
  readonly localPatientCodeRequiredError = getRequiredErrorMessage('Mã bệnh nhân cục bộ');
  readonly fullNameRequiredError = getRequiredErrorMessage('Họ tên');

  ngOnInit(): void {
    this.initForm();
  }

  private initForm(): void {
    const patient = this.patient;

    this.form = this.fb.group({
      sourceSystemId: [patient?.sourceSystemId || '', Validators.required],
      localPatientCode: [patient?.localPatientCode || '', Validators.required],
      fullName: [patient?.fullName || '', Validators.required],
      dateOfBirth: [patient?.dateOfBirth || ''],
      gender: [patient?.gender || Gender.MALE],
      nationalId: [patient?.nationalId || '', nationalIdValidator()],
      healthInsuranceNo: [patient?.healthInsuranceNo || ''],
      phoneNumber: [patient?.phoneNumber || '', phoneValidator()],
      address: [patient?.address || '']
    });
  }

  open(patient?: Patient): void {
    this.patient = patient || null;
    this.isOpen = true;
    this.document.body.classList.add('modal-open');

    if (patient) {
      this.form.patchValue({
        sourceSystemId: patient.sourceSystemId,
        localPatientCode: patient.localPatientCode,
        fullName: patient.fullName,
        dateOfBirth: patient.dateOfBirth,
        gender: patient.gender,
        nationalId: patient.nationalId,
        healthInsuranceNo: patient.healthInsuranceNo,
        phoneNumber: patient.phoneNumber,
        address: patient.address
      });
    } else {
      this.form.reset({
        sourceSystemId: '',
        localPatientCode: '',
        fullName: '',
        dateOfBirth: '',
        gender: Gender.MALE,
        nationalId: '',
        healthInsuranceNo: '',
        phoneNumber: '',
        address: ''
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
        sourceSystemId: formValue.sourceSystemId,
        localPatientCode: formValue.localPatientCode,
        fullName: formValue.fullName,
        dateOfBirth: formValue.dateOfBirth || null,
        gender: formValue.gender,
        nationalId: formValue.nationalId || null,
        healthInsuranceNo: formValue.healthInsuranceNo || null,
        phoneNumber: formValue.phoneNumber || null,
        address: formValue.address || null
      };

      this.patientService.update(request).pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe({
        next: (response) => this.handleSuccess(response),
        error: (error) => this.handleError(error)
      });
    } else {
      const request: CreatePatientRequest = {
        sourceSystemId: formValue.sourceSystemId,
        localPatientCode: formValue.localPatientCode,
        fullName: formValue.fullName,
        dateOfBirth: formValue.dateOfBirth || null,
        gender: formValue.gender,
        nationalId: formValue.nationalId || null,
        healthInsuranceNo: formValue.healthInsuranceNo || null,
        phoneNumber: formValue.phoneNumber || null,
        address: formValue.address || null
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
    if (isSuccess(response)) {
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
      getApiErrorMessage(error, 'Đã xảy ra lỗi. Vui lòng thử lại.')
    );
    this.isSubmitting = false;
  }
}