import { Injectable, inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { TokenStorageService } from '../services/token-storage.service';

export const noAuthGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const tokenStorage = inject(TokenStorageService);

  if (!tokenStorage.isLoggedIn()) {
    return true;
  }

  router.navigate(['/patients']);
  return false;
};
