import { guestGuard } from '@core/auth/guards/guest.guard';
import { Routes } from '@angular/router';

export const accessRoutes: Routes = [
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/login/login.component').then(module => module.LoginComponent)
  },
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login.component').then(module => module.LoginComponent)
  }
];
