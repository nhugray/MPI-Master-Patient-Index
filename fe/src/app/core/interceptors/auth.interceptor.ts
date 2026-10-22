import { HttpInterceptorFn, HttpErrorResponse, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, finalize, map, Observable, shareReplay, switchMap, throwError } from 'rxjs';
import { TokenStorageService } from '../services/token-storage.service';
import { AuthService } from '../../features/auth/services/auth.service';

let refresh$: Observable<string> | null = null;

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenStorage = inject(TokenStorageService);
  const authService = inject(AuthService);
  const router = inject(Router);

  if (isAuthEndpoint(req.url)) {
    return next(req.clone({ withCredentials: true }));
  }

  const token = tokenStorage.getAccessToken();

  if (!token || tokenStorage.shouldRefresh()) {
    return getNewToken(authService, tokenStorage, router).pipe(
      switchMap(newToken => next(addTokenToRequest(req, newToken)))
    );
  }

  const authReq = token ? addTokenToRequest(req, token) : req.clone({ withCredentials: true });

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401) {
        return throwError(() => error);
      }

      return getNewToken(authService, tokenStorage, router).pipe(
        switchMap(newToken => next(addTokenToRequest(req, newToken)))
      );
    })
  );
};

function getNewToken(
  authService: AuthService,
  tokenStorage: TokenStorageService,
  router: Router
): Observable<string> {
  if (!refresh$) {
    refresh$ = authService.refreshToken().pipe(
      map(response => {
        const accessToken = response?.data?.accessToken;
        if (!accessToken) {
          throw new Error('Refresh response không có accessToken');
        }
        return accessToken;
      }),
      catchError((err: HttpErrorResponse) => {
        if (err?.status === 401 || err?.status === 403) {
          tokenStorage.clear();
          router.navigate(['/login']);
        }
        return throwError(() => err);
      }),
      finalize(() => {
        refresh$ = null;
      }),
      shareReplay({ bufferSize: 1, refCount: false })
    );
  }
  return refresh$;
}

function isAuthEndpoint(url: string): boolean {
  return url.includes('/auth/login') ||
    url.includes('/auth/refresh') ||
    url.includes('/auth/logout');
}

function addTokenToRequest(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return req.clone({
    withCredentials: true,
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });
}
