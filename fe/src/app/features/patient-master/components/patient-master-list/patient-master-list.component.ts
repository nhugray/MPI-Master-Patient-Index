import { Component, EventEmitter, Output, OnInit, inject, signal, computed, DestroyRef, ChangeDetectionStrategy, effect } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { PageMeta } from '../../../../shared/models';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { PatientMasterService } from '../../services/patient-master.service';
import { PatientMaster, PatientMasterSearchRequest, defaultSearchParams } from '../../models/patient-master.model';
import { PaginationComponent } from '../../../../shared/components/pagination/pagination.component';
import { SearchService } from '../../../../shared/services/search.service';
import { Gender } from '../../../../shared/enums';
import { GENDER_OPTIONS, getGenderLabel } from '../../../../shared/enums';
import { PATIENT_STATUS_OPTIONS, PatientMasterStatusEnum } from '../../models/patient-master.constants';
import { PatientMasterEditModalComponent } from '../patient-master-form/patient-master-form.component';
import { PatientMasterFilterPanelComponent } from '../patient-master-filter-panel/patient-master-filter-panel.component';


@Component({
  selector: 'app-patient-master-list',
  standalone: true,
  imports: [
    DatePipe,
    DecimalPipe,
    FormsModule,
    PaginationComponent,
    PatientMasterEditModalComponent,
    PatientMasterFilterPanelComponent
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './patient-master-list.component.html',
  styleUrls: ['./patient-master-list.component.css']
})
export class PatientMasterListComponent implements OnInit {
  @Output() viewDetail = new EventEmitter<PatientMaster>();

  editingPatient = signal<PatientMaster | null>(null);

  private readonly patientMasterService = inject(PatientMasterService);
  private readonly toastService = inject(ToastService);
  readonly searchService = inject(SearchService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);

  patientMasters = signal<PatientMaster[]>([]);
  pageMeta = signal<PageMeta | null>(null);
  isLoading = signal(false);

  private searchParams = defaultSearchParams;

  activeCount = signal(0);
  mergedCount = signal(0);
  totalCount = signal(0);

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
    this.loadPatientMasters();
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

  onGenderFilterChange(gender: string): void {
    this.searchParams = {
      ...this.searchParams,
      gender: gender !== 'ALL' ? gender as Gender : undefined,
      page: 0
    };
    this.loadPatientMasters();
  }

  onStatusFilterChange(status: string): void {
    const params: PatientMasterSearchRequest = {
      ...this.searchParams,
      page: 0
    };

    if (status !== 'ALL') {
      params.status = status;
    } else {
      delete params.status;
    }

    this.searchParams = params;
    this.loadPatientMasters();
  }

  resetFilters(): void {
    this.searchService.clearQuery();
    this.searchParams = defaultSearchParams;
    this.loadPatientMasters();
  }

  onViewDetail(master: PatientMaster): void {
    this.router.navigate(['/patient-masters', master.id]);
  }

  openEditModal(master: PatientMaster): void {
    this.editingPatient.set(master);
  }

  closeEditModal(): void {
    this.editingPatient.set(null);
  }

  onPatientUpdated(updatedPatient: PatientMaster): void {
    // Update in the list
    this.patientMasters.update(list =>
      list.map(p => p.id === updatedPatient.id ? updatedPatient : p)
    );

    this.toastService.success('Thành công', 'Cập nhật hồ sơ gốc thành công');
    this.editingPatient.set(null);
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

  getAvatarColor = getGenderLabel;

  getGenderDisplay(gender: Gender): string {
    const option = GENDER_OPTIONS.find(o => o.value === gender);
    return option ? option.label : '-';
  }

  getStatusDisplay(status: PatientMasterStatusEnum): string {
    const option = PATIENT_STATUS_OPTIONS.find(o => o.value === status);
    return option ? option.label : '-';
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
