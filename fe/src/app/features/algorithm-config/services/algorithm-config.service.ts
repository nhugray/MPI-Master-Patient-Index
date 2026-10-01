import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { ApiResponse, PageResponse } from '../../../shared/models';
import { CreateWeightConfigRequest, WeightConfig } from '../models/algorithm-config.model';

@Injectable({
  providedIn: 'root'
})
export class AlgorithmConfigService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;
  private readonly basePath = '/algorithm-config';

  getActiveConfig(): Observable<ApiResponse<WeightConfig>> {
    return this.http.get<ApiResponse<WeightConfig>>(`${this.apiUrl}${this.basePath}/active`);
  }

  getAllConfigs(page: number, size: number): Observable<ApiResponse<PageResponse<WeightConfig>>> {
    const httpParams = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<ApiResponse<PageResponse<WeightConfig>>>(`${this.apiUrl}${this.basePath}`, {
      params: httpParams
    });
  }

  getById(id: number): Observable<ApiResponse<WeightConfig>> {
    return this.http.get<ApiResponse<WeightConfig>>(`${this.apiUrl}${this.basePath}/${id}`);
  }

  create(request: CreateWeightConfigRequest): Observable<ApiResponse<WeightConfig>> {
    return this.http.post<ApiResponse<WeightConfig>>(`${this.apiUrl}${this.basePath}`, request);
  }

  activate(id: number): Observable<ApiResponse<WeightConfig>> {
    return this.http.put<ApiResponse<WeightConfig>>(`${this.apiUrl}${this.basePath}/${id}/activate`, {});
  }
}
