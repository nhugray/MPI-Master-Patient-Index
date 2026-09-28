import { Component, EventEmitter, Output, Input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Gender } from '../../../../shared/enums';
import { GENDER_OPTIONS } from '../../../patient/models/patient.constants';
import { PatientStatusEnum } from '../../../patient/enums/patient-status.enum';

export interface SourceSystem {
  id: number;
  name: string;
}

export interface PatientMasterFilterState {
  genders: Gender[];
  statuses: PatientStatusEnum[];
  sourceSystemIds: number[];
}

@Component({
  selector: 'app-patient-master-filter-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './patient-master-filter-panel.component.html',
  styleUrls: ['./patient-master-filter-panel.component.css']
})
export class PatientMasterFilterPanelComponent {
  @Input() sourceSystems: SourceSystem[] = [];
  @Output() filterChange = new EventEmitter<PatientMasterFilterState>();
  @Output() resetFilter = new EventEmitter<void>();

  selectedGenders = new Set<Gender>();
  selectedStatuses = new Set<PatientStatusEnum>();
  selectedSourceSystems = new Set<number>();

  genderOptions = GENDER_OPTIONS;
  statusOptions = [
    { value: PatientStatusEnum.ACTIVE, label: 'Đang hoạt động' },
    { value: PatientStatusEnum.INACTIVE, label: 'Không hoạt động' },
    { value: PatientStatusEnum.MERGED, label: 'Đã gộp' },
    { value: PatientStatusEnum.DUPLICATE, label: 'Trùng lặp' }
  ];

  toggleGender(gender: Gender): void {
    if (this.selectedGenders.has(gender)) {
      this.selectedGenders.delete(gender);
    } else {
      this.selectedGenders.add(gender);
    }
  }

  toggleStatus(status: PatientStatusEnum): void {
    if (this.selectedStatuses.has(status)) {
      this.selectedStatuses.delete(status);
    } else {
      this.selectedStatuses.add(status);
    }
  }

  toggleSourceSystem(systemId: number): void {
    if (this.selectedSourceSystems.has(systemId)) {
      this.selectedSourceSystems.delete(systemId);
    } else {
      this.selectedSourceSystems.add(systemId);
    }
  }

  apply(): void {
    this.filterChange.emit({
      genders: Array.from(this.selectedGenders),
      statuses: Array.from(this.selectedStatuses),
      sourceSystemIds: Array.from(this.selectedSourceSystems)
    });
  }

  reset(): void {
    this.selectedGenders.clear();
    this.selectedStatuses.clear();
    this.selectedSourceSystems.clear();
    this.resetFilter.emit();
  }
}
