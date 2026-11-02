import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { AuthService } from '@app/auth/auth.service';

const authorize = (isAuthorized: (authService: AuthService) => boolean): boolean | UrlTree => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isLoggedIn()) {
    return router.createUrlTree(['/login']);
  }
  if (!isAuthorized(authService)) {
    return router.createUrlTree([authService.dashboardRoute()]);
  }
  return true;
};

export const playerGuard: CanActivateFn = () => authorize(authService => authService.isPlayerPlan());

export const courtAdminGuard: CanActivateFn = () => authorize(authService => authService.isCourtPlan());

export const platformAdminGuard: CanActivateFn = () => authorize(authService => authService.isPlatformAdmin());
