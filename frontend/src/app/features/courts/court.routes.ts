import { courtAdminGuard, playerGuard } from '@core/auth/guards/plan.guard';
import { Routes } from '@angular/router';

export const courtRoutes: Routes = [
  {
    path: 'canchas',
    canActivate: [playerGuard],
    loadComponent: () => import('./pages/canchas/canchas.component').then(module => module.CanchasComponent)
  },
  {
    path: 'canchas/:id',
    canActivate: [playerGuard],
    loadComponent: () => import('./pages/cancha-detail/cancha-detail.component').then(module => module.CanchaDetailComponent)
  },
  {
    path: 'admin-canchas',
    canActivate: [courtAdminGuard],
    loadComponent: () => import('./pages/admin-canchas/admin-canchas.component').then(module => module.AdminCanchasComponent)
  },
  {
    path: 'calendar',
    canActivate: [courtAdminGuard],
    loadComponent: () => import('./pages/calendar/calendar.component').then(module => module.CalendarComponent)
  },
  {
    path: 'availability',
    canActivate: [courtAdminGuard],
    loadComponent: () => import('./pages/availability/availability.component').then(module => module.AvailabilityComponent)
  },
  {
    path: 'reports',
    canActivate: [courtAdminGuard],
    loadComponent: () => import('./pages/reports/reports.component').then(module => module.ReportsComponent)
  }
];
