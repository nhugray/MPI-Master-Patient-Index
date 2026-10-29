import { Component, EventEmitter, Output, OnInit, inject, signal, DestroyRef, ChangeDetectionStrategy } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { DecimalPipe, NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { skip } from 'rxjs';

import { PageMeta, isSuccess } from '../../../../shared/models';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { UserService } from '../../services/user.service';
import { User } from '../../models/user.model';
import { UserSearchParams, defaultSearchParams } from '../../models/user-search-params.model';
import { USER_STATUS_OPTIONS, UserStatus } from '../../../../shared/enums';
import { PaginationComponent } from '../../../../shared/components/pagination/pagination.component';
import { SearchService } from '../../../../shared/services/search.service';
import { getApiErrorMessage } from '../../../../shared/utils/http-error.util';
import { toApiPage } from '../../../../shared/utils/pagination.util';
import { formatDateTime } from '../../../../shared/utils/date.util';
import { getInitials } from '../../../../shared/utils/string.util';
import { UserFilterPanelComponent } from '../user-filter-panel/user-filter-panel.component';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [
    DecimalPipe,
    NgClass,
    FormsModule,
    PaginationComponent,
    UserFilterPanelComponent
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './user-list.component.html',
  styleUrls: ['./user-list.component.css']
})
export class UserListComponent implements OnInit {
  @Output() addUser = new EventEmitter<void>();
  @Output() editUser = new EventEmitter<User>();
  @Output() deleteUser = new EventEmitter<User>();

  private readonly userService = inject(UserService);
  private readonly toastService = inject(ToastService);
  readonly searchService = inject(SearchService);
  private readonly destroyRef = inject(DestroyRef);

  users = signal<User[]>([]);
  pageMeta = signal<PageMeta | null>(null);
  isLoading = signal(false);

  selectedStatus: UserStatus | 'ALL' = 'ALL';
  statusOptions = USER_STATUS_OPTIONS;

  private searchParams: UserSearchParams = { ...defaultSearchParams };

  activeCount = signal(0);
  totalCount = signal(0);
  adminCount = signal(0);

  constructor() {
    toObservable(this.searchService.searchQuery)
      .pipe(skip(1), takeUntilDestroyed())
      .subscribe((query) => {
        this.searchParams = {
          ...this.searchParams,
          keyword: query || undefined,
          page: 0
        };
        this.loadUsers();
      });
  }

  ngOnInit(): void {
    this.loadCounts();
    this.loadUsers();
  }

  reload(): void {
    this.loadCounts();
    this.loadUsers();
  }

  loadCounts(): void {
    this.userService.getCounts().pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (response) => {
        if (isSuccess(response)) {
          const counts = response.data;
          this.totalCount.set(counts['TOTAL'] ?? counts['total'] ?? 0);
          this.activeCount.set(counts['ACTIVE'] ?? counts['active'] ?? 0);
          this.adminCount.set(counts['ADMIN'] ?? counts['admin'] ?? 0);
        }
      },
      error: (err) => {
        console.error('Error loading user counts:', err);
      }
    });
  }

  loadUsers(): void {
    this.isLoading.set(true);
    this.userService.search(this.searchParams).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (response) => {
        if (isSuccess(response)) {
          this.users.set(response.data.result);
          this.pageMeta.set(response.data.meta);

          if (this.totalCount() === 0) {
            this.totalCount.set(response.data.meta?.total || 0);
          }
        }
        this.isLoading.set(false);
      },
      error: (error: { error?: { message?: string } }) => {
        this.toastService.error('Lỗi hệ thống', getApiErrorMessage(error, 'Không thể tải danh sách người dùng'));
        this.isLoading.set(false);
      }
    });
  }

  onPageChange(page: number): void {
    this.searchParams = {
      ...this.searchParams,
      page: toApiPage(page)
    };
    this.loadUsers();
  }

  onStatusFilterChange(status: UserStatus | 'ALL'): void {
    this.selectedStatus = status;
    this.searchParams = {
      ...this.searchParams,
      status: status !== 'ALL' ? status : undefined,
      page: 0
    };
    this.loadUsers();
  }

  resetFilters(): void {
    this.selectedStatus = 'ALL';
    this.searchService.clearQuery();
    this.searchParams = {
      ...defaultSearchParams,
      page: 0
    };
    this.loadUsers();
  }

  onAddUser(): void {
    this.addUser.emit();
  }

  onEditUser(user: User): void {
    this.editUser.emit(user);
  }

  onDeleteUser(user: User): void {
    this.deleteUser.emit(user);
  }

  getStatusLabel(status: UserStatus): string {
    return USER_STATUS_OPTIONS.find(o => o.value === status)?.label || status;
  }

  getStatusIcon(status: UserStatus): string {
    return USER_STATUS_OPTIONS.find(o => o.value === status)?.icon || 'list';
  }

  getStatusClass(status: UserStatus | string): string {
    const key = String(status ?? '').toLowerCase();
    switch (key) {
      case 'active':
        return 'status-active';
      case 'suspended':
      case 'locked':
      case 'banned':
        return 'status-suspended';
      default:
        return 'status-inactive';
    }
  }

  formatDate(date: any): string {
    return formatDateTime(date);
  }

  getInitials(name: string): string {
    return getInitials(name);
  }
}