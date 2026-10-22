import { Component, EventEmitter, Output, ChangeDetectionStrategy, inject, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { USER_STATUS_OPTIONS, UserStatus, USER_STATUS_ALL_OPTION } from '../../../../shared/enums';
import { SearchService } from '../../../../shared/services/search.service';

@Component({
  selector: 'app-user-filter-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './user-filter-panel.component.html',
  styleUrls: ['./user-filter-panel.component.css']
})
export class UserFilterPanelComponent {
  readonly searchService = inject(SearchService);

  @Output() statusChange = new EventEmitter<UserStatus | 'ALL'>();
  @Output() resetFilters = new EventEmitter<void>();

  selectedStatus: UserStatus | 'ALL' = 'ALL';
  statusOptions = USER_STATUS_OPTIONS;

  statusDropdownOpen = false;

  @HostListener('document:click')
  onDocumentClick(): void {
    this.statusDropdownOpen = false;
  }

  toggleStatusDropdown(event: Event): void {
    event.stopPropagation();
    this.statusDropdownOpen = !this.statusDropdownOpen;
  }

  selectStatus(value: UserStatus | 'ALL'): void {
    this.selectedStatus = value;
    this.statusDropdownOpen = false;
    this.onStatusChange();
  }

  getStatusLabel(): string {
    if (this.selectedStatus === 'ALL') {
      return USER_STATUS_ALL_OPTION.label;
    }
    return this.statusOptions.find(s => s.value === this.selectedStatus)?.label || 'Tất cả';
  }

  getStatusIcon(value: UserStatus | 'ALL'): string {
    if (value === 'ALL') return 'list';
    switch (value) {
      case UserStatus.ACTIVE: return 'check_circle';
      case UserStatus.INACTIVE: return 'cancel';
      case UserStatus.SUSPENDED: return 'lock';
      default: return 'list';
    }
  }

  onStatusChange(): void {
    this.statusChange.emit(this.selectedStatus);
  }

  onReset(): void {
    this.selectedStatus = 'ALL';
    this.searchService.clearQuery();
    this.resetFilters.emit();
  }
}
