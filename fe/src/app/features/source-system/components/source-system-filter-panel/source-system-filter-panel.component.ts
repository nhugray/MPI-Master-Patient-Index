import { Component, EventEmitter, Output, Input, ChangeDetectionStrategy, inject, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SearchService } from '../../../../shared/services/search.service';
import { Facility } from '../../../facility/models/facility.model';

@Component({
  selector: 'app-source-system-filter-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './source-system-filter-panel.component.html',
  styleUrls: ['./source-system-filter-panel.component.css']
})
export class SourceSystemFilterPanelComponent {
  readonly searchService = inject(SearchService);

  @Input() facilities: Facility[] = [];
  @Output() facilityChange = new EventEmitter<number | null>();
  @Output() statusChange = new EventEmitter<string>();
  @Output() resetFilters = new EventEmitter<void>();

  selectedFacilityId: number | null = null;
  selectedStatus: string = 'ALL';

  statusOptions = [
    { value: 'ALL', label: 'Tất cả' },
    { value: 'ACTIVE', label: 'Đang hoạt động' },
    { value: 'INACTIVE', label: 'Không hoạt động' }
  ];

  // Dropdown states
  facilityDropdownOpen = false;
  statusDropdownOpen = false;

  @HostListener('document:click')
  onDocumentClick(): void {
    this.facilityDropdownOpen = false;
    this.statusDropdownOpen = false;
  }

  toggleFacilityDropdown(event: Event): void {
    event.stopPropagation();
    this.facilityDropdownOpen = !this.facilityDropdownOpen;
    this.statusDropdownOpen = false;
  }

  toggleStatusDropdown(event: Event): void {
    event.stopPropagation();
    this.statusDropdownOpen = !this.statusDropdownOpen;
    this.facilityDropdownOpen = false;
  }

  selectFacility(id: number | null): void {
    this.selectedFacilityId = id;
    this.facilityDropdownOpen = false;
    this.onFacilityChange();
  }

  selectStatus(value: string): void {
    this.selectedStatus = value;
    this.statusDropdownOpen = false;
    this.onStatusChange();
  }

  getFacilityLabel(): string {
    if (this.selectedFacilityId === null) return 'Tất cả cơ sở';
    const facility = this.facilities.find(f => f.id === this.selectedFacilityId);
    return facility ? facility.name : 'Tất cả cơ sở';
  }

  getStatusLabel(): string {
    return this.statusOptions.find(s => s.value === this.selectedStatus)?.label || 'Tất cả';
  }

  getStatusIcon(value: string): string {
    switch (value) {
      case 'ACTIVE': return 'check_circle';
      case 'INACTIVE': return 'cancel';
      default: return 'list';
    }
  }

  onFacilityChange(): void {
    this.facilityChange.emit(this.selectedFacilityId);
  }

  onStatusChange(): void {
    this.statusChange.emit(this.selectedStatus);
  }

  onReset(): void {
    this.selectedFacilityId = null;
    this.selectedStatus = 'ALL';
    this.searchService.clearQuery();
    this.resetFilters.emit();
  }
}
