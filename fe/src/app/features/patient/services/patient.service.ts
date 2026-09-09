import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { ApiResponse, PageResponse } from '../../../shared/models';
import {
  CreatePatientRequest,
  Patient,
  UpdatePatientRequest
} from '../models/patient.model';
import { PatientSearchParams } from '../models/patient-search-params.model';

@Injectable({
  providedIn: 'root'
})
export class PatientService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;
  private readonly basePath = '/patients';

  search(params: PatientSearchParams): Observable<ApiResponse<PageResponse<Patient>>> {
    let httpParams = new HttpParams();

    if (params.fullName) {
      httpParams = httpParams.set('fullName', params.fullName);
    }
    if (params.gender) {
      httpParams = httpParams.set('gender', params.gender);
    }
    if (params.nationalId) {
      httpParams = httpParams.set('nationalId', params.nationalId);
    }
    if (params.healthInsuranceNo) {
      httpParams = httpParams.set('healthInsuranceNo', params.healthInsuranceNo);
    }
    if (params.phoneNumber) {
      httpParams = httpParams.set('phoneNumber', params.phoneNumber);
    }
    if (params.page !== undefined) {
      httpParams = httpParams.set('page', params.page.toString());
    }
    if (params.size !== undefined) {
      httpParams = httpParams.set('size', params.size.toString());
    }
    if (params.matchStatus !== undefined) {
      httpParams = httpParams.set('matchStatus', params.matchStatus);
    }

    return this.http.get<ApiResponse<PageResponse<Patient>>>(`${this.apiUrl}${this.basePath}`, {
      params: httpParams
    });
  }

  getById(id: number): Observable<ApiResponse<Patient>> {
    return this.http.get<ApiResponse<Patient>>(`${this.apiUrl}${this.basePath}/${id}`);
  }

  create(request: CreatePatientRequest): Observable<ApiResponse<Patient>> {
    return this.http.post<ApiResponse<Patient>>(`${this.apiUrl}${this.basePath}`, request);
  }

  update(request: UpdatePatientRequest): Observable<ApiResponse<Patient>> {
    return this.http.put<ApiResponse<Patient>>(`${this.apiUrl}${this.basePath}`, request);
  }

  delete(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}${this.basePath}/${id}`);
  }
}
