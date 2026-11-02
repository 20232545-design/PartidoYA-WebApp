import { authGuard } from '@core/auth/guards/auth.guard';
import { Routes } from '@angular/router';

export const dashboardRoutes: Routes = [
  {
    path: 'home',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/home/home.component').then(module => module.HomeComponent)
  }
];
