import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { of } from 'rxjs';
import { switchMap, tap, catchError } from 'rxjs/operators';

import { AuthHeaderComponent } from '../../components/auth-header/auth-header.component';
import { AuthErrorAlertComponent } from '../../components/auth-error-alert/auth-error-alert.component';
import { LoginFormComponent } from '../../components/login-form/login-form.component';

import { AuthService } from '../../services/auth.service';
import { LoginCredentials } from '../../models/login-request.model';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [
    CommonModule,
    AuthHeaderComponent,
    AuthErrorAlertComponent,
    LoginFormComponent,
  ],
  templateUrl: './login-page.component.html',
  styleUrls: ['./login-page.component.css']
})
export class LoginPageComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  errorMessage: string | null = null;

  onLogin(credentials: LoginCredentials): void {
    this.errorMessage = null;

    this.authService.login(credentials).pipe(
      tap((response) => {
        if (response && response.data) {
          console.log('[Login] Setting token expiry:', response.data.expiresIn);
        }
      }),
      switchMap((response) => {
        if (response.error) {
          this.errorMessage = response.message || 'Đăng nhập thất bại';
          return of(null);
        }

        console.log('[Login] Success:', response.data);

        return this.authService.getMe().pipe(
          tap((userResponse) => console.log('[Login] User info:', userResponse.data)),
          catchError((err) => {
            console.error('[Login] Failed to fetch user info nhưng vẫn cho chuyển trang:', err);
            return of({ isFailedGetMe: true });
          })
        );
      })
    ).subscribe({
      next: (userResponse) => {
        if (userResponse !== null) {
          this.router.navigate(['/patients']);
        }
      },
      error: (error: HttpErrorResponse) => {
        console.error('[Login] Error:', error);

        if (error.status === 401) {
          this.errorMessage = 'Sai tên đăng nhập hoặc mật khẩu';
        } else if (error.status === 0) {
          this.errorMessage = 'Không thể kết nối đến server. Vui lòng kiểm tra kết nối mạng.';
        } else if (error.error?.message) {
          this.errorMessage = error.error.message;
        } else {
          this.errorMessage = 'Đã xảy ra lỗi. Vui lòng thử lại sau.';
        }
      }
    });
  }
}
