import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { ApiResponse, PageResponse, RoleInfo } from '../../../shared/models';
import { User, CreateUserRequest, UpdateUserRequest } from '../models/user.model';
import { UserSearchParams } from '../models/user-search-params.model';

@Injectable({

  providedIn: 'root'
})
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;
  private readonly basePath = '/users';
  private readonly rolesPath = '/roles';

  search(params: UserSearchParams): Observable<ApiResponse<PageResponse<User>>> {
    let httpParams = new HttpParams();

    if (params.keyword) {
      httpParams = httpParams.set('keyword', params.keyword);
    }
    if (params.status) {
      httpParams = httpParams.set('status', params.status);
    }
    if (params.roleId !== undefined) {
      httpParams = httpParams.set('roleId', params.roleId.toString());
    }
    if (params.page !== undefined) {
      httpParams = httpParams.set('page', params.page.toString());
    }
    if (params.size !== undefined) {
      httpParams = httpParams.set('size', params.size.toString());
    }

    return this.http.get<ApiResponse<PageResponse<User>>>(`${this.apiUrl}${this.basePath}`, {
      params: httpParams
    });
  }

  getById(id: number): Observable<ApiResponse<User>> {
    return this.http.get<ApiResponse<User>>(`${this.apiUrl}${this.basePath}/${id}`);
  }

  create(request: CreateUserRequest): Observable<ApiResponse<User>> {
    return this.http.post<ApiResponse<User>>(`${this.apiUrl}${this.basePath}`, request);
  }

  update(request: UpdateUserRequest): Observable<ApiResponse<User>> {
    return this.http.put<ApiResponse<User>>(`${this.apiUrl}${this.basePath}`, request);
  }

  delete(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}${this.basePath}/${id}`);
  }

  getAllRoles(): Observable<ApiResponse<PageResponse<RoleInfo>>> {
    return this.http.get<ApiResponse<PageResponse<RoleInfo>>>(`${this.apiUrl}${this.rolesPath}`, {
      params: new HttpParams().set('page', '0').set('size', '100')
    });
  }
}
