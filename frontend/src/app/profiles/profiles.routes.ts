import { authGuard } from '@app/auth/auth.guard';
import { Routes } from '@angular/router';

export const profileRoutes: Routes = [
  {
    path: 'mi-perfil',
    canActivate: [authGuard],
    loadComponent: () => import('./profile-page/profile-page.component').then(module => module.ProfilePageComponent)
  },
  {
    path: 'profile',
    redirectTo: 'mi-perfil'
  }
];
