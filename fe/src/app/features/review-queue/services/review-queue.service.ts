import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApiResponse, PageResponse } from '../../../shared/models';
import { MatchCandidate, ReviewQueueSearchRequest, ReviewDecisionRequest, MatchDecisionEnum } from '../models/match-candidate.model';
import { environment } from '../../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ReviewQueueService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;
  private readonly basePath = '/review-queue';

  search(params: ReviewQueueSearchRequest): Observable<ApiResponse<PageResponse<MatchCandidate>>> {
    let httpParams = new HttpParams()
      .set('page', params.page.toString())
      .set('size', params.size.toString());

    if (params.decision != null) {
      httpParams = httpParams.set('decision', params.decision);
    }

    return this.http.get<ApiResponse<PageResponse<MatchCandidate>>>(`${this.apiUrl}${this.basePath}`, { params: httpParams });
  }

  getById(id: number): Observable<ApiResponse<MatchCandidate>> {
    return this.http.get<ApiResponse<MatchCandidate>>(`${this.apiUrl}${this.basePath}/${id}`);
  }

  submitDecision(id: number, request: ReviewDecisionRequest): Observable<ApiResponse<MatchCandidate>> {
    return this.http.put<ApiResponse<MatchCandidate>>(`${this.apiUrl}${this.basePath}/${id}/decision`, request);
  }

  getCounts(): Observable<ApiResponse<{ [key: string]: number }>> {
    return this.http.get<ApiResponse<{ [key: string]: number }>>(`${this.apiUrl}${this.basePath}/counts`);
  }
}
