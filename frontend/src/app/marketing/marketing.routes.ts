import { Routes } from '@angular/router';

export const marketingRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./landing/landing.component').then(module => module.LandingComponent)
  },
  {
    path: 'contactanos',
    loadComponent: () => import('./contact/contact.component').then(module => module.ContactComponent)
  },
  {
    path: 'planes-jugador',
    loadComponent: () => import('./player-plans/player-plans.component').then(module => module.PlayerPlansComponent)
  },
  {
    path: 'planes-cancha',
    loadComponent: () => import('./court-plans/court-plans.component').then(module => module.CourtPlansComponent)
  }
];
