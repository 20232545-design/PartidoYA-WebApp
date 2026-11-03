import { guestGuard } from '@app/auth/guest.guard';
import { Routes } from '@angular/router';

export const authRoutes: Routes = [
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () => import('./register/register.component').then(module => module.RegisterComponent)
  },
  {
    path: 'login',
    loadComponent: () => import('./login/login.component').then(module => module.LoginComponent)
  }
];
