import { Component, EventEmitter, Output, Input, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Gender, MatchStatus } from '../../../../shared/enums';
import { GENDER_OPTIONS } from '../../../../shared/enums';
import { MATCH_STATUS_OPTIONS } from '../../models/patient.constants';
import { SearchService } from '../../../../shared/services/search.service';
import { SourceSystem } from '../../../source-system/models/source-system.model';

@Component({
  selector: 'app-patient-filter-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './patient-filter-panel.component.html',
  styleUrls: ['./patient-filter-panel.component.css']
})
export class PatientFilterPanelComponent {
  readonly searchService = inject(SearchService);

  @Input() sourceSystems: SourceSystem[] = [];
  @Output() genderChange = new EventEmitter<string>();
  @Output() matchStatusChange = new EventEmitter<string>();
  @Output() sourceSystemChange = new EventEmitter<number | null>();
  @Output() resetFilters = new EventEmitter<void>();

  selectedGender: string = 'ALL';
  selectedMatchStatus: string = 'ALL';
  selectedSourceSystemId: number | null = null;

  genderOptions = GENDER_OPTIONS;
  matchStatusOptions = MATCH_STATUS_OPTIONS;

  onGenderChange(): void {
    this.genderChange.emit(this.selectedGender);
  }

  onMatchStatusChange(): void {
    this.matchStatusChange.emit(this.selectedMatchStatus);
  }

  onSourceSystemChange(): void {
    this.sourceSystemChange.emit(this.selectedSourceSystemId);
  }

  onReset(): void {
    this.selectedGender = 'ALL';
    this.selectedMatchStatus = 'ALL';
    this.selectedSourceSystemId = null;
    this.searchService.clearQuery();
    this.resetFilters.emit();
  }
}
