import { Component, EventEmitter, Output, ChangeDetectionStrategy, inject, HostListener } from '@angular/core';
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
  statusOptions = [
    { value: 'ALL', label: 'Tất cả' },
    { value: 'ACTIVE', label: 'Đang hoạt động' },
    { value: 'INACTIVE', label: 'Ngừng hoạt động' }
  ];

  // Dropdown states
  typeDropdownOpen = false;
  statusDropdownOpen = false;

  @HostListener('document:click')
  onDocumentClick(): void {
    this.typeDropdownOpen = false;
    this.statusDropdownOpen = false;
  }

  toggleTypeDropdown(event: Event): void {
    event.stopPropagation();
    this.typeDropdownOpen = !this.typeDropdownOpen;
    this.statusDropdownOpen = false;
  }

  toggleStatusDropdown(event: Event): void {
    event.stopPropagation();
    this.statusDropdownOpen = !this.statusDropdownOpen;
    this.typeDropdownOpen = false;
  }

  selectType(value: FacilityType | 'ALL'): void {
    this.selectedType = value;
    this.typeDropdownOpen = false;
    this.onTypeChange();
  }

  selectStatus(value: string): void {
    this.selectedStatus = value;
    this.statusDropdownOpen = false;
    this.onStatusChange();
  }

  getTypeLabel(): string {
    return this.facilityTypeOptions.find(t => t.value === this.selectedType)?.label || 'Tất cả';
  }

  getStatusLabel(): string {
    return this.statusOptions.find(s => s.value === this.selectedStatus)?.label || 'Tất cả';
  }

  getTypeIcon(value: FacilityType | 'ALL'): string {
    switch (value) {
      case FacilityType.HOSPITAL: return 'local_hospital';
      case FacilityType.CLINIC: return 'medical_services';
      case FacilityType.OTHER: return 'apartment';
      default: return 'category';
    }
  }

  getStatusIcon(value: string): string {
    switch (value) {
      case 'ACTIVE': return 'check_circle';
      case 'INACTIVE': return 'cancel';
      default: return 'list';
    }
  }

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
