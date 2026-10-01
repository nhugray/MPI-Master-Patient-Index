import { Component, EventEmitter, Input, Output, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';

import { WeightConfig } from '../../models/algorithm-config.model';
import { formatDateTime } from '../../../../shared/utils/date.util';

@Component({
  selector: 'app-weight-history-table',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './weight-history-table.component.html',
  styleUrls: ['./weight-history-table.component.css']
})
export class WeightHistoryTableComponent {
  @Input() configs: WeightConfig[] = [];
  @Input() isLoading = false;

  @Output() activate = new EventEmitter<number>();

  formatWeights(config: WeightConfig): string {
    return `${config.nameWeight} / ${config.dobWeight} / ${config.nationalIdWeight} / ${config.phoneWeight} / ${config.addressWeight}`;
  }

  formatDate(dateString: string | null): string {
    if (!dateString) return '-';
    return formatDateTime(dateString).replace(' ', ' - ');
  }

  onActivate(id: number): void {
    this.activate.emit(id);
  }
}
