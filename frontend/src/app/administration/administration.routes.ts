import { platformAdminGuard } from '@app/auth/plan.guard';
import { Routes } from '@angular/router';

export const administrationRoutes: Routes = [
  {
    path: 'admin-users',
    canActivate: [platformAdminGuard],
    loadComponent: () => import('./user-management/user-management.component').then(module => module.UserManagementComponent)
  },
  {
    path: 'admin-reportes',
    canActivate: [platformAdminGuard],
    loadComponent: () => import('./player-reports/player-reports.component').then(module => module.PlayerReportsComponent)
  }
];
