import { playerGuard } from '@app/auth/plan.guard';
import { Routes } from '@angular/router';

export const matchRoutes: Routes = [
  {
    path: 'deportes',
    canActivate: [playerGuard],
    loadComponent: () => import('./match-list/match-list.component').then(module => module.MatchListComponent)
  },
  {
    path: 'mis-partidos',
    canActivate: [playerGuard],
    loadComponent: () => import('./my-matches/my-matches.component').then(module => module.MyMatchesComponent)
  },
  {
    path: 'partidos/:id',
    canActivate: [playerGuard],
    loadComponent: () => import('./match-detail/match-detail.component').then(module => module.MatchDetailComponent)
  }
];
