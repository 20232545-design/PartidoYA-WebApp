import { courtAdminGuard, courtBusinessGuard, playerGuard } from '@app/auth/plan.guard';
import { Routes } from '@angular/router';

export const courtRoutes: Routes = [
  {
    path: 'canchas',
    canActivate: [playerGuard],
    loadComponent: () => import('./court-list/court-list.component').then(module => module.CourtListComponent)
  },
  {
    path: 'canchas/:id',
    canActivate: [playerGuard],
    loadComponent: () => import('./court-detail/court-detail.component').then(module => module.CourtDetailComponent)
  },
  {
    path: 'admin-canchas',
    canActivate: [courtAdminGuard],
    loadComponent: () => import('./court-management/court-management.component').then(module => module.CourtManagementComponent)
  },
  {
    path: 'calendar',
    canActivate: [courtAdminGuard],
    loadComponent: () => import('./calendar/calendar.component').then(module => module.CalendarComponent)
  },
  {
    path: 'availability',
    canActivate: [courtAdminGuard],
    loadComponent: () => import('./availability/availability.component').then(module => module.AvailabilityComponent)
  },
  {
    path: 'reports',
    canActivate: [courtBusinessGuard],
    loadComponent: () => import('./reports/reports.component').then(module => module.ReportsComponent)
  }
];
