import { authGuard } from '@app/auth/auth.guard';
import { Routes } from '@angular/router';

export const notificationRoutes: Routes = [
  {
    path: 'notificaciones',
    canActivate: [authGuard],
    loadComponent: () => import('./notification-list/notification-list.component').then(module => module.NotificationListComponent)
  },
  {
    path: 'notificaciones/:matchId/:requestId',
    canActivate: [authGuard],
    loadComponent: () => import('./notification-detail/notification-detail.component').then(module => module.NotificationDetailComponent)
  }
];
