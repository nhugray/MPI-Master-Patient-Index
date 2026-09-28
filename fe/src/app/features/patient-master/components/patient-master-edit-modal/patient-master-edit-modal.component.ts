import { Component, EventEmitter, Input, Output, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { PatientMaster, UpdatePatientMasterRequest } from '../../models/patient-master.model';
import { PatientMasterService } from '../../services/patient-master.service';
import { Gender } from '../../../../shared/enums';

@Component({
  selector: 'app-patient-master-edit-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './patient-master-edit-modal.component.html',
  styleUrls: ['./patient-master-edit-modal.component.css']
})
export class PatientMasterEditModalComponent implements OnInit {
  @Input() patientMaster!: PatientMaster;
  @Output() close = new EventEmitter<void>();
  @Output() saved = new EventEmitter<PatientMaster>();

  editForm!: FormGroup;
  isSubmitting = signal(false);
  errorMessage = signal<string | null>(null);

  genderOptions = [
    { value: '', label: 'Chọn giới tính' },
    { value: Gender.MALE, label: 'Nam' },
    { value: Gender.FEMALE, label: 'Nữ' },
    { value: Gender.OTHER, label: 'Khác' }
  ];

  constructor(
    private fb: FormBuilder,
    private patientMasterService: PatientMasterService
  ) {}

  ngOnInit(): void {
    this.initForm();
  }

  private initForm(): void {
    this.editForm = this.fb.group({
      fullName: [this.patientMaster.fullName, [Validators.required, Validators.maxLength(255)]],
      dateOfBirth: [this.patientMaster.dateOfBirth, [Validators.required]],
      gender: [this.patientMaster.gender || ''],
      nationalId: [this.patientMaster.nationalId || '', [Validators.pattern(/^\d{12}$/)]],
      healthInsuranceNo: [this.patientMaster.healthInsuranceNo || '', [Validators.maxLength(20)]],
      phoneNumber: [this.patientMaster.phoneNumber || '', [Validators.pattern(/^0\d{9}$/)]],
      address: [this.patientMaster.address || '', [Validators.maxLength(500)]]
    });
  }

  onSubmit(): void {
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const request: UpdatePatientMasterRequest = {
      id: this.patientMaster.id,
      ...this.editForm.value
    };

    this.patientMasterService.update(request).subscribe({
      next: (response) => {
        this.isSubmitting.set(false);
        this.saved.emit(response.data);
      },
      error: (error) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(error.error?.message || 'Có lỗi xảy ra khi cập nhật');
      }
    });
  }

  onClose(): void {
    this.close.emit();
  }

  getFieldError(fieldName: string): string | null {
    const field = this.editForm.get(fieldName);
    if (field?.invalid && field?.touched) {
      if (field.errors?.['required']) return 'Trường này là bắt buộc';
      if (field.errors?.['maxLength']) return `Tối đa ${field.errors?.['maxLength'].requiredLength} ký tự`;
      if (field.errors?.['pattern']) {
        if (fieldName === 'nationalId') return 'CCCD phải gồm 12 chữ số';
        if (fieldName === 'phoneNumber') return 'Số ĐT phải gồm 10 chữ số và bắt đầu bằng 0';
      }
    }
    return null;
  }
}
