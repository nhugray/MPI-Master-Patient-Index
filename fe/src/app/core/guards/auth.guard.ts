import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of, switchMap } from 'rxjs';
import { TokenStorageService } from '../services/token-storage.service';
import { AuthService } from '../../features/auth/services/auth.service';

export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  const tokenStorage = inject(TokenStorageService);
  const authService = inject(AuthService);

  const loginUrl = router.createUrlTree(['/login']);

  if (tokenStorage.isLoggedIn()) {
    if (!tokenStorage.getCurrentUser()) {
      return authService.getMe().pipe(
        map(() => true),
        catchError(() => {
          tokenStorage.clear();
          return of(loginUrl);
        })
      );
    }
    return true;
  }

  return authService.refreshToken().pipe(
    switchMap(() => authService.getMe()),
    map(() => true),
    catchError(() => {
      tokenStorage.clear();
      return of(loginUrl);
    })
  );
};
