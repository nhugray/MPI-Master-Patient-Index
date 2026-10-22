import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, finalize } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { TokenStorageService } from '../../../core/services/token-storage.service';
import { ApiResponse } from '../../../shared/models';

import { LoginRequest, LoginCredentials } from '../models/login-request.model';
import { LoginResponse } from '../models/login-response.model';
import { AuthUser } from '../models/auth-user.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly tokenStorage = inject(TokenStorageService);
  private readonly apiUrl = environment.apiUrl;
  private readonly apiPrefix = '/auth';

  login(credentials: LoginCredentials): Observable<ApiResponse<LoginResponse>> {
    const request: LoginRequest = {
      email: credentials.email,
      password: credentials.password
    };

    return this.http.post<ApiResponse<LoginResponse>>(
      `${this.apiUrl}${this.apiPrefix}/login`,
      request,
      { withCredentials: true }
    ).pipe(
      tap(response => {
        if (response && response.data) {
          this.tokenStorage.setAccessToken(response.data.accessToken);

          this.tokenStorage.setTokenExpiry(response.data.expiresIn);

          if (credentials.rememberMe) {
            this.tokenStorage.setRememberEmail(credentials.email);
          } else {
            this.tokenStorage.clearRememberEmail();
          }
        }
      })
    );
  }

  logout(): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(
      `${this.apiUrl}${this.apiPrefix}/logout`,
      {},
      { withCredentials: true }
    ).pipe(
      finalize(() => {
        this.tokenStorage.clear();
      })
    );
  }

  refreshToken(): Observable<ApiResponse<LoginResponse>> {
    return this.http.post<ApiResponse<LoginResponse>>(
      `${this.apiUrl}${this.apiPrefix}/refresh`,
      {},
      { withCredentials: true }
    ).pipe(
      tap(response => {
        if (response && response.data) {
          this.tokenStorage.setAccessToken(response.data.accessToken);

          this.tokenStorage.setTokenExpiry(response.data.expiresIn);
        }
      })
    );
  }

  getMe(): Observable<ApiResponse<AuthUser>> {
    return this.http.get<ApiResponse<AuthUser>>(
      `${this.apiUrl}${this.apiPrefix}/me`,
      { withCredentials: true }
    ).pipe(
      tap(response => {
        if (response && response.data) {
          this.tokenStorage.setCurrentUser(response.data);
        }
      })
    );
  }

  changePassword(payload: {
    currentPassword: string;
    newPassword: string;
    confirmPassword: string;
  }): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(
      `${this.apiUrl}${this.apiPrefix}/change-password`,
      payload,
      { withCredentials: true }
    );
  }

  isLoggedIn(): boolean {
    return this.tokenStorage.isLoggedIn();
  }

  getCurrentUser(): AuthUser | null {
    return this.tokenStorage.getCurrentUser();
  }

  getAccessToken(): string | null {
    return this.tokenStorage.getAccessToken();
  }

  getRememberEmail(): string | null {
    return this.tokenStorage.getRememberEmail();
  }
}
