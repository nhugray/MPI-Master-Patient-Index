import { Component, EventEmitter, Output, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FacilityType } from '../../models/facility.model';
import { FACILITY_TYPE_OPTIONS } from '../../models/facility.constants';
import { SearchService } from '../../../../shared/services/search.service';

@Component({
  selector: 'app-facility-filter-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './facility-filter-panel.component.html',
  styleUrls: ['./facility-filter-panel.component.css']
})
export class FacilityFilterPanelComponent {
  readonly searchService = inject(SearchService);

  @Output() typeChange = new EventEmitter<FacilityType | 'ALL'>();
  @Output() statusChange = new EventEmitter<string>();
  @Output() resetFilters = new EventEmitter<void>();

  selectedType: FacilityType | 'ALL' = 'ALL';
  selectedStatus: string = 'ALL';

  facilityTypeOptions = FACILITY_TYPE_OPTIONS;

  onTypeChange(): void {
    this.typeChange.emit(this.selectedType);
  }

  onStatusChange(): void {
    this.statusChange.emit(this.selectedStatus);
  }

  onReset(): void {
    this.selectedType = 'ALL';
    this.selectedStatus = 'ALL';
    this.searchService.clearQuery();
    this.resetFilters.emit();
  }
}
