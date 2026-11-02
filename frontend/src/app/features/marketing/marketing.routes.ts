import { Routes } from '@angular/router';

export const marketingRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/landing/landing.component').then(module => module.LandingComponent)
  },
  {
    path: 'contactanos',
    loadComponent: () => import('./pages/contactanos/contactanos.component').then(module => module.ContactanosComponent)
  },
  {
    path: 'planes-jugador',
    loadComponent: () => import('./pages/planes-jugador/planes-jugador.component').then(module => module.PlanesJugadorComponent)
  },
  {
    path: 'planes-cancha',
    loadComponent: () => import('./pages/planes-cancha/planes-cancha.component').then(module => module.PlanesCanchaComponent)
  }
];
