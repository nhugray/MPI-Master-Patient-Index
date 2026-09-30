import { Component } from '@angular/core';
import { ReviewQueueListComponent } from '../components/review-queue-list/review-queue-list.component';

@Component({
  selector: 'app-review-queue-page',
  standalone: true,
  imports: [ReviewQueueListComponent],
  template: `<app-review-queue-list></app-review-queue-list>`,
  styles: []
})
export class ReviewQueuePageComponent {}
