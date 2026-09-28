import { Component, EventEmitter, Output, OnInit, inject, signal, computed, DestroyRef, ChangeDetectionStrategy, effect } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { PageMeta } from '../../../../shared/models';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { PatientMasterService } from '../../services/patient-master.service';
import { PatientMaster, PatientMasterSearchRequest } from '../../models/patient-master.model';
import { PaginationComponent } from '../../../../shared/components/pagination/pagination.component';
import { SearchService } from '../../../../shared/services/search.service';
import { Gender } from '../../../../shared/enums';
import { SourceSystemService } from '../../../source-system/services/source-system.service';
import { SourceSystem } from '../../../source-system/models/source-system.model';
import { PatientMasterFilterPanelComponent, PatientMasterFilterState } from '../patient-master-filter-panel/patient-master-filter-panel.component';

@Component({
  selector: 'app-patient-master-list',
  standalone: true,
  imports: [
    DatePipe,
    DecimalPipe,
    FormsModule,
    PaginationComponent,
    PatientMasterFilterPanelComponent
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './patient-master-list.component.html',
  styleUrls: ['./patient-master-list.component.css']
})
export class PatientMasterListComponent implements OnInit {
  @Output() viewDetail = new EventEmitter<PatientMaster>();
  @Output() editMaster = new EventEmitter<PatientMaster>();

  private readonly patientMasterService = inject(PatientMasterService);
  private readonly sourceSystemService = inject(SourceSystemService);
  private readonly toastService = inject(ToastService);
  readonly searchService = inject(SearchService);
  private readonly destroyRef = inject(DestroyRef);

  patientMasters = signal<PatientMaster[]>([]);
  pageMeta = signal<PageMeta | null>(null);
  isLoading = signal(false);

  private searchParams: PatientMasterSearchRequest = {
    page: 0,
    size: 10
  };

  activeCount = signal(0);
  mergedCount = signal(0);
  totalCount = signal(0);

  // Filter options
  genderOptions = [
    { value: 'ALL', label: 'Tất cả' },
    { value: Gender.MALE, label: 'Nam' },
    { value: Gender.FEMALE, label: 'Nữ' },
    { value: Gender.OTHER, label: 'Khác' }
  ];

  statusOptions = [
    { value: 'ALL', label: 'Tất cả' },
    { value: 'ACTIVE', label: 'Hoạt động' },
    { value: 'INACTIVE', label: 'Ngừng hoạt động' },
    { value: 'MERGED', label: 'Đã gộp' }
  ];

  sourceSystems = signal<SourceSystem[]>([]);

  // Selected filters
  selectedGender: string = 'ALL';
  selectedStatus: string = 'ALL';
  selectedSourceSystemId: number | null = null;

  // Computed signals
  readonly totalMasters = computed(() => this.totalCount());
  readonly activeMasters = computed(() => this.activeCount());
  readonly mergedMasters = computed(() => this.mergedCount());
  readonly todayNew = computed(() => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return this.patientMasters().filter(m => {
      const created = new Date(m.createdAt);
      return created >= today;
    }).length;
  });

  constructor() {
    effect(() => {
      const query = this.searchService.searchQuery();
      this.searchParams = {
        ...this.searchParams,
        keyword: query || undefined,
        page: 0
      };
      this.loadPatientMasters();
    }, { allowSignalWrites: true });
  }

  ngOnInit(): void {
    this.loadSourceSystems();
    this.loadPatientMasters();
  }

  loadSourceSystems(): void {
    this.sourceSystemService.search({ isActive: true, page: 0, size: 100 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          if (response.statusCode === 200) {
            this.sourceSystems.set(response.data.result);
          }
        },
        error: (error) => {
          console.error('Error loading source systems:', error);
        }
      });
  }

  reload(): void {
    this.loadPatientMasters();
  }

  loadPatientMasters(): void {
    this.isLoading.set(true);
    this.patientMasterService.search(this.searchParams).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (response) => {
        if (response.statusCode === 200) {
          this.patientMasters.set(response.data.result);
          this.pageMeta.set(response.data.meta);

          const total = response.data.meta?.total || 0;
          this.totalCount.set(total);
          this.activeCount.set(
            response.data.result.filter((m: PatientMaster) => m.status === 'ACTIVE').length
          );
          this.mergedCount.set(
            response.data.result.filter((m: PatientMaster) => m.status === 'MERGED').length
          );
        }
        this.isLoading.set(false);
      },
      error: (error: { error?: { message?: string } }) => {
        this.toastService.error('Lỗi hệ thống', error?.error?.message || 'Không thể tải danh sách hồ sơ gốc');
        this.isLoading.set(false);
      }
    });
  }

  onPageChange(page: number): void {
    this.searchParams = {
      ...this.searchParams,
      page: page - 1
    };
    this.loadPatientMasters();
  }

  onGenderFilterChange(): void {
    this.searchParams = {
      ...this.searchParams,
      gender: this.selectedGender !== 'ALL' ? this.selectedGender : undefined,
      page: 0
    };
    this.loadPatientMasters();
  }

  onStatusFilterChange(): void {
    this.searchParams = {
      ...this.searchParams,
      status: this.selectedStatus !== 'ALL' ? this.selectedStatus : undefined,
      page: 0
    };
    this.loadPatientMasters();
  }

  onSourceSystemFilterChange(): void {
    this.searchParams = {
      ...this.searchParams,
      sourceSystemIds: this.selectedSourceSystemId ? [this.selectedSourceSystemId] : undefined,
      page: 0
    };
    this.loadPatientMasters();
  }

  resetFilters(): void {
    this.selectedGender = 'ALL';
    this.selectedStatus = 'ALL';
    this.selectedSourceSystemId = null;
    this.searchService.clearQuery();
    this.searchParams = {
      page: 0,
      size: 10
    };
    this.loadPatientMasters();
  }

  onViewDetail(master: PatientMaster): void {
    this.viewDetail.emit(master);
  }

  onEditMaster(master: PatientMaster): void {
    this.editMaster.emit(master);
  }

  // Helper methods
  getInitials(fullName: string): string {
    if (!fullName) return '?';
    const words = fullName.trim().split(' ');
    if (words.length === 1) {
      return words[0].charAt(0).toUpperCase();
    }
    return (words[0].charAt(0) + words[words.length - 1].charAt(0)).toUpperCase();
  }

  getAvatarColor(gender: string): string {
    switch (gender) {
      case 'MALE': return 'male';
      case 'FEMALE': return 'female';
      default: return 'other';
    }
  }

  getGenderDisplay(gender: string): string {
    switch (gender) {
      case 'MALE': return 'Nam';
      case 'FEMALE': return 'Nữ';
      case 'OTHER': return 'Khác';
      default: return '-';
    }
  }

  getStatusDisplay(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'Hoạt động';
      case 'INACTIVE': return 'Ngừng hoạt động';
      case 'MERGED': return 'Đã gộp';
      default: return status;
    }
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'status-active';
      case 'INACTIVE': return 'status-inactive';
      case 'MERGED': return 'status-merged';
      default: return '';
    }
  }

  maskNationalId(id?: string): string {
    if (!id) return '-';
    if (id.length <= 4) return id;
    return id.substring(0, 3) + '***' + id.substring(id.length - 3);
  }

  maskHealthInsurance(insurance?: string): string {
    if (!insurance) return '-';
    if (insurance.length <= 6) return insurance;
    return insurance.substring(0, 4) + '***' + insurance.substring(insurance.length - 4);
  }

  maskPhoneNumber(phone?: string): string {
    if (!phone) return '-';
    if (phone.length <= 6) return phone;
    return phone.substring(0, 3) + '***' + phone.substring(phone.length - 3);
  }

  getTruncatedAddress(address?: string): string {
    if (!address) return '-';
    return address.length > 50 ? address.substring(0, 50) + '...' : address;
  }
}
