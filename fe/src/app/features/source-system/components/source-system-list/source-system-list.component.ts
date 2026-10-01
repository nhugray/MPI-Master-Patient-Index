import { Component, EventEmitter, Output, OnInit, inject, signal, DestroyRef, ChangeDetectionStrategy, effect } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { PageMeta, isSuccess } from '../../../../shared/models';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { SourceSystemService } from '../../services/source-system.service';
import { SourceSystem } from '../../models/source-system.model';
import { SourceSystemSearchParams, defaultSearchParams } from '../../models/source-system-search-params.model';
import { PaginationComponent } from '../../../../shared/components/pagination/pagination.component';
import { SearchService } from '../../../../shared/services/search.service';
import { FacilityService } from '../../../facility/services/facility.service';
import { Facility } from '../../../facility/models/facility.model';
import { SourceSystemFilterPanelComponent } from '../source-system-filter-panel/source-system-filter-panel.component';
import { getApiErrorMessage } from '../../../../shared/utils/http-error.util';
import { toApiPage } from '../../../../shared/utils/pagination.util';

@Component({
  selector: 'app-source-system-list',
  standalone: true,
  imports: [
    DecimalPipe,
    FormsModule,
    PaginationComponent,
    SourceSystemFilterPanelComponent,
    PaginationComponent,
    SourceSystemFilterPanelComponent
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './source-system-list.component.html',
  styleUrls: ['./source-system-list.component.css']
})
export class SourceSystemListComponent implements OnInit {
  @Output() addSourceSystem = new EventEmitter<void>();
  @Output() editSourceSystem = new EventEmitter<SourceSystem>();
  @Output() deleteSourceSystem = new EventEmitter<SourceSystem>();

  private readonly sourceSystemService = inject(SourceSystemService);
  private readonly facilityService = inject(FacilityService);
  private readonly toastService = inject(ToastService);
  readonly searchService = inject(SearchService);
  private readonly destroyRef = inject(DestroyRef);

  sourceSystems = signal<SourceSystem[]>([]);
  pageMeta = signal<PageMeta | null>(null);
  isLoading = signal(false);

  facilities = signal<Facility[]>([]);
  selectedFacilityId: number | null = null;
  selectedStatus: string = 'ALL';

  private searchParams: SourceSystemSearchParams = { ...defaultSearchParams };

  activeCount = signal(0);
  inactiveCount = signal(0);
  totalCount = signal(0);

  constructor() {
    effect(() => {
      const query = this.searchService.searchQuery();
      this.searchParams = {
        ...this.searchParams,
        name: query || undefined,
        page: 0
      };
      this.loadSourceSystems();
    }, { allowSignalWrites: true });
  }

  ngOnInit(): void {
    this.loadDropdownData();
    this.loadSourceSystems();
  }

  loadDropdownData(): void {
    this.facilityService.search({ isActive: true, page: 0, size: 100 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          if (isSuccess(response)) {
            this.facilities.set(response.data.result);
          }
        },
        error: (error) => {
          console.error('Error loading facilities:', error);
        }
      });
  }

  onFacilityFilterChange(facilityId: number | null): void {
    this.selectedFacilityId = facilityId;
    this.searchParams = {
      ...this.searchParams,
      facilityId: facilityId !== null ? facilityId : undefined,
      page: 0
    };
    this.loadSourceSystems();
  }


  reload(): void {
    this.loadSourceSystems();
  }

  loadSourceSystems(): void {
    this.isLoading.set(true);
    this.sourceSystemService.search(this.searchParams).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (response) => {
        if (isSuccess(response)) {
          this.sourceSystems.set(response.data.result);
          this.pageMeta.set(response.data.meta);

          const total = response.data.meta?.total || 0;
          this.totalCount.set(total);
          this.activeCount.set(
            response.data.result.filter((s: SourceSystem) => s.isActive).length
          );
          this.inactiveCount.set(
            response.data.result.filter((s: SourceSystem) => !s.isActive).length
          );
        }
        this.isLoading.set(false);
      },
      error: (error: { error?: { message?: string } }) => {
        this.toastService.error('Lỗi hệ thống', getApiErrorMessage(error, 'Không thể tải danh sách hệ thống nguồn'));
        this.isLoading.set(false);
      }
    });
  }

  onPageChange(page: number): void {
    this.searchParams = {
      ...this.searchParams,
      page: toApiPage(page)
    };
    this.loadSourceSystems();
  }

  onStatusFilterChange(status: string): void {
    this.selectedStatus = status;
    this.searchParams = {
      ...this.searchParams,
      isActive: status === 'ACTIVE' ? true : status === 'INACTIVE' ? false : undefined,
      page: 0
    };
    this.loadSourceSystems();
  }

  resetFilters(): void {
    this.selectedFacilityId = null;
    this.selectedStatus = 'ALL';
    this.searchService.clearQuery();
    this.searchParams = {
      ...defaultSearchParams,
      page: 0
    };
    this.loadSourceSystems();
  }

  onAddSourceSystem(): void {
    this.addSourceSystem.emit();
  }

  onEditSourceSystem(sourceSystem: SourceSystem): void {
    this.editSourceSystem.emit(sourceSystem);
  }

  onDeleteSourceSystem(sourceSystem: SourceSystem): void {
    this.deleteSourceSystem.emit(sourceSystem);
  }

  getStatusClass(isActive: boolean): string {
    return isActive ? 'active' : 'inactive';
  }

  getStatusLabel(isActive: boolean): string {
    return isActive ? 'Đang hoạt động' : 'Không hoạt động';
  }
}
