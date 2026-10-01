import { Component, OnInit, inject, signal, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { AlgorithmConfigService } from '../services/algorithm-config.service';
import { WeightConfig, CreateWeightConfigRequest } from '../models/algorithm-config.model';
import { WeightEditorPanelComponent } from '../components/weight-editor-panel/weight-editor-panel.component';
import { WeightHistoryTableComponent } from '../components/weight-history-table/weight-history-table.component';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { getApiErrorMessage } from '../../../shared/utils/http-error.util';
import { isSuccess } from '../../../shared/models';

@Component({
  selector: 'app-algorithm-config-page',
  standalone: true,
  imports: [CommonModule, WeightEditorPanelComponent, WeightHistoryTableComponent],
  templateUrl: './algorithm-config-page.component.html',
  styleUrls: ['./algorithm-config-page.component.css']
})
export class AlgorithmConfigPageComponent implements OnInit {
  private readonly algorithmConfigService = inject(AlgorithmConfigService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  activeConfig = signal<WeightConfig | null>(null);
  historyConfigs = signal<WeightConfig[]>([]);
  isLoadingActive = signal(false);
  isLoadingHistory = signal(false);
  isSaving = signal(false);

  ngOnInit(): void {
    this.loadActiveConfig();
    this.loadHistory();
  }

  loadActiveConfig(): void {
    this.isLoadingActive.set(true);
    this.algorithmConfigService.getActiveConfig().pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (response) => {
        if (isSuccess(response)) {
          this.activeConfig.set(response.data);
        }
        this.isLoadingActive.set(false);
      },
      error: () => {
        this.isLoadingActive.set(false);
      }
    });
  }

  loadHistory(): void {
    this.isLoadingHistory.set(true);
    this.algorithmConfigService.getAllConfigs(0, 20).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (response) => {
        if (isSuccess(response)) {
          this.historyConfigs.set(response.data.result);
        }
        this.isLoadingHistory.set(false);
      },
      error: () => {
        this.toastService.error('Lỗi', 'Không thể tải lịch sử cấu hình trọng số');
        this.isLoadingHistory.set(false);
      }
    });
  }

  onSave(request: CreateWeightConfigRequest): void {
    this.isSaving.set(true);
    this.algorithmConfigService.create(request).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (response) => {
        if (isSuccess(response)) {
          this.toastService.success('Thành công', `Đã tạo phiên bản mới ${response.data.version}`);
          this.loadHistory();
        }
        this.isSaving.set(false);
      },
      error: (error) => {
        this.toastService.error('Lỗi', getApiErrorMessage(error, 'Không thể lưu cấu hình trọng số'));
        this.isSaving.set(false);
      }
    });
  }

  onActivate(id: number): void {
    this.algorithmConfigService.activate(id).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (response) => {
        if (isSuccess(response)) {
          this.toastService.success('Thành công', `Đã kích hoạt phiên bản ${response.data.version}`);
          this.activeConfig.set(response.data);
          this.loadHistory();
        }
      },
      error: (error) => {
        this.toastService.error('Lỗi', getApiErrorMessage(error, 'Không thể kích hoạt phiên bản này'));
      }
    });
  }
}
