import { playerGuard } from '@core/auth/guards/plan.guard';
import { Routes } from '@angular/router';

export const matchRoutes: Routes = [
  {
    path: 'deportes',
    canActivate: [playerGuard],
    loadComponent: () => import('./pages/deportes/deportes.component').then(module => module.DeportesComponent)
  },
  {
    path: 'mis-partidos',
    canActivate: [playerGuard],
    loadComponent: () => import('./pages/mis-partidos/mis-partidos.component').then(module => module.MisPartidosComponent)
  },
  {
    path: 'partidos/:id',
    canActivate: [playerGuard],
    loadComponent: () => import('./pages/match-detail/match-detail.component').then(module => module.MatchDetailComponent)
  }
];
