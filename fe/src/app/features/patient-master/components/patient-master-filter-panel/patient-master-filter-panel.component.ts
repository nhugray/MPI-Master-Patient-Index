import { Component, EventEmitter, Output, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Gender } from '../../../../shared/enums';
import { GENDER_OPTIONS } from '../../../../shared/enums';
import { PATIENT_STATUS_OPTIONS } from '../../models/patient-master.constants';
import { SearchService } from '../../../../shared/services/search.service';

@Component({
  selector: 'app-patient-master-filter-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './patient-master-filter-panel.component.html',
  styleUrls: ['./patient-master-filter-panel.component.css']
})
export class PatientMasterFilterPanelComponent {
  readonly searchService = inject(SearchService);

  @Output() genderChange = new EventEmitter<string>();
  @Output() statusChange = new EventEmitter<string>();
  @Output() resetFilters = new EventEmitter<void>();

  selectedGender: string = 'ALL';
  selectedStatus: string = 'ALL';

  genderOptions = GENDER_OPTIONS;
  statusOptions = PATIENT_STATUS_OPTIONS;

  onGenderChange(): void {
    this.genderChange.emit(this.selectedGender);
  }

  onStatusChange(): void {
    this.statusChange.emit(this.selectedStatus);
  }

  onReset(): void {
    this.selectedGender = 'ALL';
    this.selectedStatus = 'ALL';
    this.resetFilters.emit();
  }
}
