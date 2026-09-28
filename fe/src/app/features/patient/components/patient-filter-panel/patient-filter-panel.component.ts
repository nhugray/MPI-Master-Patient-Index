import { Component, EventEmitter, Output, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Gender, MatchStatus } from '../../../../shared/enums';
import { GENDER_OPTIONS } from '../../../../shared/enums';
import { MATCH_STATUS_OPTIONS } from '../../models/patient.constants';

export interface FilterState {
  fullName: string;
  gender: Gender | null;
  matchStatus: MatchStatus | null;
}

@Component({
  selector: 'app-patient-filter-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './patient-filter-panel.component.html',
  styleUrls: ['./patient-filter-panel.component.css']
})
export class PatientFilterPanelComponent {
  @Output() filterChange = new EventEmitter<{ genders: Gender[]; matchStatuses: MatchStatus[] }>();
  @Output() resetFilter = new EventEmitter<void>();

  selectedGenders = new Set<Gender>();
  selectedMatchStatuses = new Set<MatchStatus>();

  genderOptions = GENDER_OPTIONS;
  matchStatusOptions = MATCH_STATUS_OPTIONS;

  toggleGender(gender: Gender): void {
    if (this.selectedGenders.has(gender)) {
      this.selectedGenders.delete(gender);
    } else {
      this.selectedGenders.add(gender);
    }
  }

  toggleMatchStatus(status: MatchStatus): void {
    if (this.selectedMatchStatuses.has(status)) {
      this.selectedMatchStatuses.delete(status);
    } else {
      this.selectedMatchStatuses.add(status);
    }
  }

  apply(): void {
    this.filterChange.emit({
      genders: Array.from(this.selectedGenders),
      matchStatuses: Array.from(this.selectedMatchStatuses)
    });
  }

  reset(): void {
    this.selectedGenders.clear();
    this.selectedMatchStatuses.clear();
    this.resetFilter.emit();
  }
}