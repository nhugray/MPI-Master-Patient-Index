import { Component, OnInit, inject, signal, computed, DestroyRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ReviewQueueService } from '../../services/review-queue.service';
import { MatchCandidate, MatchDecisionEnum, ScoreBreakdownItem } from '../../models/match-candidate.model';
import { ToastService } from '../../../../shared/components/toast/toast.service';

interface FieldComparison {
  label: string;
  key: string;
  patientValue: string;
  masterValue: string;
  isMatch: boolean;
  matchPercent: number;
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

    return [
      this.buildComparison('Họ và tên', patient.fullName, master.fullName),
      this.buildComparison('Ngày sinh', this.formatDate(patient.dateOfBirth), this.formatDate(master.dateOfBirth)),
      this.buildComparison('CCCD/CMND', patient.nationalId || '', master.nationalId || ''),
      this.buildComparison('Số điện thoại', patient.phoneNumber || '', master.phoneNumber || ''),
      this.buildComparison('Địa chỉ', patient.address || '', master.address || ''),
      this.buildComparison('BHYT', patient.healthInsuranceNo || '', master.healthInsuranceNo || '')
    ];
  });

  overallScore = computed(() => {
    const c = this.candidate();
    return c ? Math.round(c.matchScore) : 0;
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
        if (response.statusCode === 200) {
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

  private buildComparison(label: string, patientValue: string, masterValue: string): FieldComparison {
    const isMatch = this.normalizeValue(patientValue) === this.normalizeValue(masterValue);
    const hasValues = !!patientValue && !!masterValue;
    return {
      label,
      key: label,
      patientValue: patientValue || '[Trống]',
      masterValue: masterValue || '[Trống]',
      isMatch: hasValues && isMatch,
      matchPercent: !hasValues ? 0 : (isMatch ? 100 : 0)
    };
  }

  private normalizeValue(value: string): string {
    return (value || '').trim().toLowerCase();
  }

  formatDate(dateString: string): string {
    if (!dateString) return '';
    const date = new Date(dateString);
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    return `${day}/${month}/${year}`;
  }

  goBack(): void {
    this.router.navigate(['/review-queue']);
  }

  onApprove(): void {
    this.submitDecision(MatchDecisionEnum.MANUAL_APPROVED, 'Đã duyệt gộp hồ sơ thành công');
  }

  onReject(): void {
    this.submitDecision(MatchDecisionEnum.REJECTED, 'Đã từ chối gộp hồ sơ');
  }

  private submitDecision(decision: MatchDecisionEnum, successMessage: string): void {
    const id = this.candidateId();
    if (!id || this.isSubmitting()) return;

    this.isSubmitting.set(true);

    // TODO: reviewerId nên lấy từ AuthService/session hiện tại, tạm dùng giá trị mặc định
    this.reviewQueueService.submitDecision(id, {
      decision,
      reviewNote: this.reviewNote() || undefined,
      reviewerId: 1
    }).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (response) => {
        if (response.statusCode === 200) {
          this.toastService.success('Thành công', successMessage);
          this.router.navigate(['/review-queue']);
        }
        this.isSubmitting.set(false);
      },
      error: (error) => {
        this.toastService.error('Lỗi hệ thống', error?.error?.message || 'Không thể xử lý quyết định duyệt');
        this.isSubmitting.set(false);
      }
    });
  }
}
