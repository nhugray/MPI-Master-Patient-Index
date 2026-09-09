import { Component, EventEmitter, Output, OnInit, inject, signal, DestroyRef, ChangeDetectionStrategy, effect } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { PageMeta } from '../../../../shared/models';
import { Gender, MatchStatus } from '../../../../shared/enums';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { PatientService } from '../../services/patient.service';
import { Patient } from '../../models/patient.model';
import { PatientSearchParams, defaultSearchParams } from '../../models/patient-search-params.model';
import { GENDER_OPTIONS, MATCH_STATUS_OPTIONS } from '../../models/patient.constants';
import { PatientFilterPanelComponent } from '../patient-filter-panel/patient-filter-panel.component';
import { PaginationComponent } from '../../../../shared/components/pagination/pagination.component';
import { SearchService } from '../../../../shared/services/search.service';
import { formatPhoneNumber } from '../../../../shared/validators/phone.validator';

@Component({
  selector: 'app-patient-list',
  standalone: true,
  imports: [
    DatePipe,
    DecimalPipe,
    FormsModule,
    PatientFilterPanelComponent,
    PaginationComponent
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './patient-list.component.html',
  styleUrls: ['./patient-list.component.css']
})
export class PatientListComponent implements OnInit {
  @Output() addPatient = new EventEmitter<void>();
  @Output() editPatient = new EventEmitter<Patient>();
  @Output() deletePatient = new EventEmitter<Patient>();

  private readonly patientService = inject(PatientService);
  private readonly toastService = inject(ToastService);
  private readonly searchService = inject(SearchService);
  private readonly destroyRef = inject(DestroyRef);

  patients = signal<Patient[]>([]);
  pageMeta = signal<PageMeta | null>(null);
  isLoading = signal(false);
  isFilterOpen = signal(false);

  private searchParams: PatientSearchParams = { ...defaultSearchParams };

  pendingCount = signal(0);
  matchedCount = signal(0);

  constructor() {
    effect(() => {
      const query = this.searchService.searchQuery();
      this.searchParams = {
        ...this.searchParams,
        fullName: query || undefined,
        page: 0
      };
      this.loadPatients();
    }, { allowSignalWrites: true });
  }

  ngOnInit(): void {
    this.loadPatients();
  }

  reload(): void {
    this.loadPatients();
  }

  loadPatients(): void {
    this.isLoading.set(true);
    this.patientService.search(this.searchParams).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (response) => {
        if (response.statusCode === 200) {
          this.patients.set(response.data.result);
          this.pageMeta.set(response.data.meta);

          this.pendingCount.set(
            response.data.result.filter((p: Patient) => p.matchStatus === MatchStatus.PENDING).length
          );
          this.matchedCount.set(
            response.data.result.filter((p: Patient) => p.matchStatus === MatchStatus.MATCHED).length
          );
        }
        this.isLoading.set(false);
      },
      error: (error: { error?: { message?: string } }) => {
        this.toastService.error('Lỗi hệ thống', error?.error?.message || 'Không thể tải danh sách bệnh nhân');
        this.isLoading.set(false);
      }
    });
  }

  onPageChange(page: number): void {
    this.searchParams = {
      ...this.searchParams,
      page: page - 1
    };
    this.loadPatients();
  }

  toggleFilter(): void {
    this.isFilterOpen.set(!this.isFilterOpen());
  }

  onFilterChange(filters: { genders: Gender[]; matchStatuses: MatchStatus[] }): void {
    this.searchParams = {
      ...this.searchParams,
      gender: filters.genders.length > 0 ? filters.genders[0] : undefined,
      matchStatus: filters.matchStatuses.length > 0 ? filters.matchStatuses[0] : undefined,
      page: 0
    };
    this.isFilterOpen.set(false);
    this.loadPatients();
  }

  onResetFilter(): void {
    this.searchParams = {
      ...defaultSearchParams,
      page: 0
    };
    this.isFilterOpen.set(false);
    this.loadPatients();
  }

  onAddPatient(): void {
    this.addPatient.emit();
  }

  onEditPatient(patient: Patient): void {
    this.editPatient.emit(patient);
  }

  onDeletePatient(patient: Patient): void {
    this.deletePatient.emit(patient);
  }

  getInitials(name: string): string {
    if (!name) return '?';
    const parts = name.trim().split(/\s+/);
    if (parts.length >= 2) {
      return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }
    return parts[0].substring(0, 2).toUpperCase();
  }

  getAvatarColor(gender: Gender): string {
    switch (gender) {
      case Gender.MALE: return 'male';
      case Gender.FEMALE: return 'female';
      case Gender.OTHER: return 'other';
    }
  }

  getGenderLabel(gender: Gender): string {
    return GENDER_OPTIONS.find(o => o.value === gender)?.label || gender;
  }

  getMatchStatusLabel(status: MatchStatus): string {
    return MATCH_STATUS_OPTIONS.find(o => o.value === status)?.label || status;
  }

  formatPhone = formatPhoneNumber;
}