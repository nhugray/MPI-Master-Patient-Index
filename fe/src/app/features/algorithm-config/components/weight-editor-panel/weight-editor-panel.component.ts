import { Component, EventEmitter, Input, Output, computed, signal, effect, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { WeightConfig, WeightFieldKey, CreateWeightConfigRequest } from '../../models/algorithm-config.model';
import { WEIGHT_FIELDS, TOTAL_WEIGHT_EXPECTED, DEFAULT_WEIGHT_FORM, DEFAULT_AUTO_APPROVAL_THRESHOLD, DEFAULT_MANUAL_REVIEW_THRESHOLD } from '../../models/algorithm-config.constants';

@Component({
  selector: 'app-weight-editor-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './weight-editor-panel.component.html',
  styleUrls: ['./weight-editor-panel.component.css']
})
export class WeightEditorPanelComponent {
  @Input() set activeConfig(value: WeightConfig | null) {
    if (value) {
      this.weights.set({
        nameWeight: value.nameWeight,
        dobWeight: value.dobWeight,
        nationalIdWeight: value.nationalIdWeight,
        phoneWeight: value.phoneWeight,
        addressWeight: value.addressWeight
      });
      this.autoApprovalThreshold.set(value.autoApprovalThreshold);
      this.manualReviewThreshold.set(value.manualReviewThreshold);
      this._activeConfig.set(value);
    }
  }

  @Input() isSaving = false;

  @Output() save = new EventEmitter<CreateWeightConfigRequest>();

  readonly weightFields = WEIGHT_FIELDS;

  private readonly _activeConfig = signal<WeightConfig | null>(null);

  weights = signal<Record<WeightFieldKey, number>>({ ...DEFAULT_WEIGHT_FORM });
  autoApprovalThreshold = signal<number>(DEFAULT_AUTO_APPROVAL_THRESHOLD);
  manualReviewThreshold = signal<number>(DEFAULT_MANUAL_REVIEW_THRESHOLD);
  description = signal<string>('');

  totalWeight = computed(() => {
    const w = this.weights();
    return Object.values(w).reduce((sum, v) => sum + (Number(v) || 0), 0);
  });

  isTotalValid = computed(() => Math.abs(this.totalWeight() - TOTAL_WEIGHT_EXPECTED) < 0.01);

  activeVersion = computed(() => this._activeConfig()?.version ?? '—');

  updateWeight(key: WeightFieldKey, value: number): void {
    this.weights.update(current => ({ ...current, [key]: value }));
  }

  getWeight(key: WeightFieldKey): number {
    return this.weights()[key];
  }

  onSliderInput(key: WeightFieldKey, event: Event): void {
    const target = event.target as HTMLInputElement;
    this.updateWeight(key, Number(target.value));
  }

  onSave(): void {
    if (!this.isTotalValid() || this.isSaving) {
      return;
    }

    const request: CreateWeightConfigRequest = {
      ...this.weights(),
      autoApprovalThreshold: this.autoApprovalThreshold(),
      manualReviewThreshold: this.manualReviewThreshold(),
      description: this.description() || null,
      createdBy: null
    };

    this.save.emit(request);
  }
}
