import { authGuard } from '@app/auth/auth.guard';
import { Routes } from '@angular/router';

export const profileRoutes: Routes = [
  {
    path: 'mi-perfil',
    canActivate: [authGuard],
    loadComponent: () => import('./player-profile/player-profile.component').then(module => module.PlayerProfileComponent)
  },
  {
    path: 'profile',
    redirectTo: 'mi-perfil'
  }
];
