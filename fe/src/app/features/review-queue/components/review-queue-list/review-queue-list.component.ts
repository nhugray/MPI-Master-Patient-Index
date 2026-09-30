import { Component, inject, signal, OnInit, DestroyRef } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ReviewQueueService } from '../../services/review-queue.service';
import { MatchCandidate, ReviewQueueSearchRequest, MatchDecisionEnum, defaultReviewQueueSearchParams } from '../../models/match-candidate.model';
import { MATCH_DECISION_OPTIONS, getScoreLevel, getDecisionLabel } from '../../models/match-candidate.constants';
import { PageMeta } from '../../../../shared/models';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { PaginationComponent } from '../../../../shared/components/pagination/pagination.component';

@Component({
  selector: 'app-review-queue-list',
  standalone: true,
  imports: [CommonModule, DatePipe, PaginationComponent],
  templateUrl: './review-queue-list.component.html',
  styleUrls: ['./review-queue-list.component.css']
})
export class ReviewQueueListComponent implements OnInit {
  private readonly reviewQueueService = inject(ReviewQueueService);
  private readonly router = inject(Router);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  candidates = signal<MatchCandidate[]>([]);
  pageMeta = signal<PageMeta | null>(null);
  isLoading = signal(false);

  counts = signal<{ [key: string]: number }>({
    TOTAL: 0,
    PENDING: 0,
    AUTO_APPROVED: 0,
    MANUAL_APPROVED: 0,
    REJECTED: 0
  });

  selectedDecision = signal<MatchDecisionEnum | 'ALL'>('ALL');
  readonly decisionOptions = MATCH_DECISION_OPTIONS;

  private searchParams: ReviewQueueSearchRequest = defaultReviewQueueSearchParams;

  ngOnInit(): void {
    this.loadCounts();
    this.loadCandidates();
  }

  loadCounts(): void {
    this.reviewQueueService.getCounts().pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (response) => {
        if (response.statusCode === 200) {
          this.counts.set(response.data);
        }
      },
      error: () => {
        // Silent fail for counts
      }
    });
  }

  loadCandidates(): void {
    this.isLoading.set(true);
    this.reviewQueueService.search(this.searchParams).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (response) => {
        if (response.statusCode === 200) {
          this.candidates.set(response.data.result);
          this.pageMeta.set(response.data.meta);
        }
        this.isLoading.set(false);
      },
      error: (error) => {
        this.toastService.error('Lỗi', 'Không thể tải danh sách hồ sơ duyệt');
        this.isLoading.set(false);
      }
    });
  }

  onDecisionTabChange(decision: MatchDecisionEnum | 'ALL'): void {
    this.selectedDecision.set(decision);
    this.searchParams = {
      ...this.searchParams,
      decision: decision === 'ALL' ? undefined : decision,
      page: 0
    };
    this.loadCandidates();
  }

  onPageChange(page: number): void {
    this.searchParams = {
      ...this.searchParams,
      page: page - 1
    };
    this.loadCandidates();
  }

  onCompareClick(candidate: MatchCandidate): void {
    this.router.navigate(['/review-queue', candidate.id]);
  }

  getScoreLevel = getScoreLevel;
  getDecisionLabel = getDecisionLabel;

  formatDate(dateString: string): string {
    if (!dateString) return '-';
    const date = new Date(dateString);
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${day}/${month}/${year} ${hours}:${minutes}`;
  }

  getDecisionClass(decision: MatchDecisionEnum): string {
    switch (decision) {
      case MatchDecisionEnum.PENDING: return 'status-pending';
      case MatchDecisionEnum.AUTO_APPROVED: return 'status-auto-approved';
      case MatchDecisionEnum.MANUAL_APPROVED: return 'status-manual-approved';
      case MatchDecisionEnum.REJECTED: return 'status-rejected';
      default: return '';
    }
  }
}
