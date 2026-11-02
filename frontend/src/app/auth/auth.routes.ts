import { guestGuard } from '@app/auth/guest.guard';
import { Routes } from '@angular/router';

export const authRoutes: Routes = [
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () => import('./login/login.component').then(module => module.LoginComponent)
  },
  {
    path: 'login',
    loadComponent: () => import('./login/login.component').then(module => module.LoginComponent)
  }
];
