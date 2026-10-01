import { Component, EventEmitter, Output, ChangeDetectionStrategy, inject, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { getGenderLabel } from '../../../../shared/enums';
import { GENDER_OPTIONS } from '../../../../shared/enums';
import { PATIENT_STATUS_OPTIONS, PatientMasterStatusEnum } from '../../models/patient-master.constants';
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

  // Dropdown states
  genderDropdownOpen = false;
  statusDropdownOpen = false;

  @HostListener('document:click')
  onDocumentClick(): void {
    this.genderDropdownOpen = false;
    this.statusDropdownOpen = false;
  }

  toggleGenderDropdown(event: Event): void {
    event.stopPropagation();
    this.genderDropdownOpen = !this.genderDropdownOpen;
    this.statusDropdownOpen = false;
  }

  toggleStatusDropdown(event: Event): void {
    event.stopPropagation();
    this.statusDropdownOpen = !this.statusDropdownOpen;
    this.genderDropdownOpen = false;
  }

  selectGender(value: string): void {
    this.selectedGender = value;
    this.genderDropdownOpen = false;
    this.onGenderChange();
  }

  selectStatus(value: string): void {
    this.selectedStatus = value;
    this.statusDropdownOpen = false;
    this.onStatusChange();
  }

  getGenderLabel(): string {
    return this.genderOptions.find(g => g.value === this.selectedGender)?.label || 'Tất cả';
  }

  getStatusLabel(): string {
    return this.statusOptions.find(s => s.value === this.selectedStatus)?.label || 'Tất cả';
  }

  getGenderIcon = getGenderLabel;

  getStatusIcon(value: string): string {
    switch (value) {
      case PatientMasterStatusEnum.ACTIVE: return 'check_circle';
      case PatientMasterStatusEnum.INACTIVE: return 'cancel';
      case PatientMasterStatusEnum.MERGED: return 'merge';
      case PatientMasterStatusEnum.DUPLICATE: return 'content_copy';
      default: return 'list';
    }
  }

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
