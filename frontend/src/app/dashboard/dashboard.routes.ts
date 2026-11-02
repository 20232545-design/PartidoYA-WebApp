import { authGuard } from '@app/auth/auth.guard';
import { Routes } from '@angular/router';

export const dashboardRoutes: Routes = [
  {
    path: 'home',
    canActivate: [authGuard],
    loadComponent: () => import('./player-dashboard/player-dashboard.component').then(module => module.PlayerDashboardComponent)
  }
];
