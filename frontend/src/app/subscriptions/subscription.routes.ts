import { Routes } from '@angular/router';
import { authGuard } from '@app/auth/auth.guard';

export const subscriptionRoutes: Routes = [
  {
    path: 'mi-suscripcion',
    canActivate: [authGuard],
    loadComponent: () => import('./my-subscription/my-subscription.component').then(module => module.MySubscriptionComponent)
  }
];
