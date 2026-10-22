import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class TokenStorageService {
  private readonly TOKEN_KEY = 'access_token';
  private readonly USER_KEY = 'current_user';
  private readonly TOKEN_EXPIRY_KEY = 'token_expiry';
  private readonly REMEMBER_ME_KEY = 'remember_me_email';

  constructor() {}

  setAccessToken(token: string): void {
    sessionStorage.setItem(this.TOKEN_KEY, token);
  }

  getAccessToken(): string | null {
    return sessionStorage.getItem(this.TOKEN_KEY);
  }

  setCurrentUser(user: any): void {
    sessionStorage.setItem(this.USER_KEY, JSON.stringify(user));
  }

  getCurrentUser(): any {
    const user = sessionStorage.getItem(this.USER_KEY);
    return user ? JSON.parse(user) : null;
  }

  setTokenExpiry(expiresIn: number): void {
    const expiry = Date.now() + expiresIn;
    sessionStorage.setItem(this.TOKEN_EXPIRY_KEY, expiry.toString());
  }

  private getTimeRemaining(): number {
    const expiry = sessionStorage.getItem(this.TOKEN_EXPIRY_KEY);
    if (!expiry) {
      return 0;
    }
    const expiryTime = parseInt(expiry, 10);
    if (Number.isNaN(expiryTime)) {
      return 0;
    }
    return Math.max(0, expiryTime - Date.now());
  }

  isAccessTokenExpired(): boolean {
    return this.getTimeRemaining() <= 0;
  }

  shouldRefresh(): boolean {
    const REFRESH_THRESHOLD = 5 * 60 * 1000;
    return this.getTimeRemaining() < REFRESH_THRESHOLD;
  }

  setRememberEmail(email: string): void {
    localStorage.setItem(this.REMEMBER_ME_KEY, email);
  }

  getRememberEmail(): string | null {
    return localStorage.getItem(this.REMEMBER_ME_KEY);
  }

  clearRememberEmail(): void {
    localStorage.removeItem(this.REMEMBER_ME_KEY);
  }

  isLoggedIn(): boolean {
    const token = this.getAccessToken();
    return !!token && !this.isAccessTokenExpired();
  }

  clear(): void {
    sessionStorage.removeItem(this.TOKEN_KEY);
    sessionStorage.removeItem(this.USER_KEY);
    sessionStorage.removeItem(this.TOKEN_EXPIRY_KEY);
  }
}
