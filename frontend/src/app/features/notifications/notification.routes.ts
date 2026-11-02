import { authGuard } from '@core/auth/guards/auth.guard';
import { Routes } from '@angular/router';

export const notificationRoutes: Routes = [
  {
    path: 'notificaciones',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/notificaciones/notificaciones.component').then(module => module.NotificacionesComponent)
  },
  {
    path: 'notificaciones/:matchId/:requestId',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/notificacion-detalle/notificacion-detalle.component').then(module => module.NotificacionDetalleComponent)
  }
];
