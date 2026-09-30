import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, PageResponse } from '../../../shared/models';
import { PatientMaster, PatientMasterSearchRequest, UpdatePatientMasterRequest } from '../models/patient-master.model';
import { PatientMasterDetail, LinkedIdentifier, SourceRecord, MergeHistoryLog } from '../models/patient-master-detail.model';
import { environment } from '../../../../environments/environment';


@Injectable({
  providedIn: 'root'
})
export class PatientMasterService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;
  private readonly basePath = '/patient-masters';

  search(params: PatientMasterSearchRequest): Observable<ApiResponse<PageResponse<PatientMaster>>> {
    let httpParams = new HttpParams()
      .set('page', params.page.toString())
      .set('size', params.size.toString());

    if (params.keyword) {
      httpParams = httpParams.set('keyword', params.keyword);
    }
    if (params.gender) {
      httpParams = httpParams.set('gender', params.gender);
    }
    if (params.status) {
      httpParams = httpParams.set('status', params.status);
    }


    return this.http.get<ApiResponse<PageResponse<PatientMaster>>>(`${this.apiUrl}${this.basePath}`, {
      params: httpParams
    });
  }

  getById(id: number): Observable<ApiResponse<PatientMaster>> {
    return this.http.get<ApiResponse<PatientMaster>>(`${this.apiUrl}${this.basePath}/${id}`);
  }

  getDetail(id: number): Observable<ApiResponse<PatientMasterDetail>> {
    return this.http.get<ApiResponse<PatientMasterDetail>>(`${this.apiUrl}${this.basePath}/${id}`);
  }

  getLinkedPatients(masterId: number, page: number = 0, size: number = 10): Observable<ApiResponse<PageResponse<SourceRecord>>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<ApiResponse<PageResponse<SourceRecord>>>(`${this.apiUrl}${this.basePath}/${masterId}/patients`, { params });
  }

  update(data: UpdatePatientMasterRequest): Observable<ApiResponse<PatientMaster>> {
    return this.http.put<ApiResponse<PatientMaster>>(`${this.apiUrl}${this.basePath}/${data.id}`, data);
  }
}
