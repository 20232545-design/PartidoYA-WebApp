import { platformAdminGuard } from '@core/auth/guards/plan.guard';
import { Routes } from '@angular/router';

export const administrationRoutes: Routes = [
  {
    path: 'admin-users',
    canActivate: [platformAdminGuard],
    loadComponent: () => import('./pages/user-management/user-management.component').then(module => module.UserManagementComponent)
  }
];
