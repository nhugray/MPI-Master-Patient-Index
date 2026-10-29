import { Component, OnInit, inject, signal, computed, DestroyRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ReviewQueueService } from '../../services/review-queue.service';
import { MatchCandidate, MatchDecisionEnum, ScoreBreakdownItem } from '../../models/match-candidate.model';
import { AuthService } from '../../../auth/services/auth.service';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { formatDate } from '../../../../shared/utils/date.util';
import { normalizeString } from '../../../../shared/utils/string.util';
import { getApiErrorMessage } from '../../../../shared/utils/http-error.util';
import { isSuccess } from '../../../../shared/models';

interface FieldComparison {
  label: string;
  key: string;
  patientValue: string;
  masterValue: string;
  isMatch: boolean;
  isPartial: boolean;
  score: number;
  maxScore: number;
}

@Component({
  selector: 'app-review-queue-compare-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './review-queue-compare-page.component.html',
  styleUrls: ['./review-queue-compare-page.component.css']
})
export class ReviewQueueComparePageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly reviewQueueService = inject(ReviewQueueService);
  private readonly authService = inject(AuthService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  candidate = signal<MatchCandidate | null>(null);
  isLoading = signal(true);
  isSubmitting = signal(false);
  reviewNote = signal('');

  candidateId = signal<number | null>(null);

  fieldComparisons = computed<FieldComparison[]>(() => {
    const c = this.candidate();
    if (!c) return [];

    const patient = c.patient;
    const master = c.candidateMaster;

    const breakdownMap = new Map<string, ScoreBreakdownItem>();
    if (c.scoreBreakdown) {
      try {
        const items: ScoreBreakdownItem[] = typeof c.scoreBreakdown === 'string'
          ? JSON.parse(c.scoreBreakdown)
          : c.scoreBreakdown;
        if (Array.isArray(items)) {
          items.forEach(item => breakdownMap.set(item.field, item));
        }
      } catch (e) {
        console.error('Failed to parse scoreBreakdown', e);
      }
    }

    return [
      this.buildComparison('Họ và tên', 'fullName', patient.fullName, master.fullName, breakdownMap.get('fullName'), 30),
      this.buildComparison(
        'Ngày sinh',
        'dateOfBirth',
        patient.dateOfBirth ? this.formatDate(patient.dateOfBirth) : '',
        master.dateOfBirth ? this.formatDate(master.dateOfBirth) : '',
        breakdownMap.get('dateOfBirth'),
        25
      ),
      this.buildComparison('CCCD/CMND', 'nationalId', patient.nationalId || '', master.nationalId || '', breakdownMap.get('nationalId'), 31),
      this.buildComparison('Số điện thoại', 'phoneNumber', patient.phoneNumber || '', master.phoneNumber || '', breakdownMap.get('phoneNumber'), 5),
      this.buildComparison('Địa chỉ', 'address', patient.address || '', master.address || '', breakdownMap.get('address'), 9),
    ];
  });

  overallScore = computed(() => {
    const c = this.candidate();
    if (!c || c.matchScore == null) return 0;
    return c.matchScore;
  });

  ngOnInit(): void {
    this.route.params.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      const id = +params['id'];
      if (id) {
        this.candidateId.set(id);
        this.loadCandidate(id);
      }
    });
  }

  loadCandidate(id: number): void {
    this.isLoading.set(true);
    this.reviewQueueService.getById(id).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (response) => {
        if (isSuccess(response)) {
          this.candidate.set(response.data);
        }
        this.isLoading.set(false);
      },
      error: () => {
        this.toastService.error('Lỗi', 'Không thể tải thông tin hồ sơ so sánh');
        this.isLoading.set(false);
      }
    });
  }

  private buildComparison(
    label: string,
    key: string,
    patientValue: string,
    masterValue: string,
    breakdownItem?: ScoreBreakdownItem,
    defaultMaxScore: number = 0
  ): FieldComparison {
    const pVal = (patientValue || '').trim();
    const mVal = (masterValue || '').trim();
    const hasValues = !!pVal && !!mVal;

    let score = 0;
    let maxScore = defaultMaxScore;

    if (breakdownItem) {
      score = Number(breakdownItem.score) || 0;
      maxScore = Number(breakdownItem.maxScore) || defaultMaxScore;
    } else {
      const isExact = this.normalizeValue(pVal) === this.normalizeValue(mVal);
      score = (hasValues && isExact) ? maxScore : 0;
    }

    const isMatch = hasValues && score === maxScore && maxScore > 0;
    const isPartial = hasValues && score > 0 && score < maxScore;

    return {
      label,
      key,
      patientValue: patientValue || '[Trống]',
      masterValue: masterValue || '[Trống]',
      isMatch,
      isPartial,
      score,
      maxScore
    };
  }

  private normalizeValue = normalizeString;

  formatDate = formatDate;

  goBack(): void {
    this.router.navigate(['/review-queue']);
  }

  onApprove(): void {
    const isPreviouslyRejected = this.candidate()?.decision === MatchDecisionEnum.REJECTED;
    const msg = isPreviouslyRejected
      ? 'Đã duyệt gộp hồ sơ và xóa hồ sơ gốc tạo nhầm thành công'
      : 'Đã duyệt gộp hồ sơ thành công';
    this.submitDecision(MatchDecisionEnum.MANUAL_APPROVED, msg);
  }

  onReject(): void {
    this.submitDecision(MatchDecisionEnum.REJECTED, 'Đã từ chối gộp hồ sơ và tạo hồ sơ gốc mới thành công');
  }

  private submitDecision(decision: MatchDecisionEnum, successMessage: string): void {
    const id = this.candidateId();
    if (!id || this.isSubmitting()) return;

    this.isSubmitting.set(true);

    const currentUser = this.authService.getCurrentUser();
    const reviewerId = currentUser?.id;

    this.reviewQueueService.submitDecision(id, {
      decision,
      reviewNote: this.reviewNote() || undefined,
      reviewerId
    }).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (response) => {
        if (isSuccess(response)) {
          this.toastService.success('Thành công', successMessage);
          this.router.navigate(['/review-queue']);
        }
        this.isSubmitting.set(false);
      },
      error: (error) => {
        this.toastService.error('Lỗi hệ thống', getApiErrorMessage(error, 'Không thể xử lý quyết định duyệt'));
        this.isSubmitting.set(false);
      }
    });
  }
}
