import { authGuard } from '@core/auth/guards/auth.guard';
import { Routes } from '@angular/router';

export const profileRoutes: Routes = [
  {
    path: 'mi-perfil',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/mi-perfil/mi-perfil.component').then(module => module.MiPerfilComponent)
  },
  {
    path: 'profile',
    redirectTo: 'mi-perfil'
  }
];
