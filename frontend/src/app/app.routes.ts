import { Routes } from '@angular/router';
import { authRoutes } from '@app/auth/auth.routes';
import { administrationRoutes } from '@app/administration/administration.routes';
import { courtRoutes } from '@app/courts/courts.routes';
import { dashboardRoutes } from '@app/dashboard/dashboard.routes';
import { marketingRoutes } from '@app/marketing/marketing.routes';
import { matchRoutes } from '@app/matches/matches.routes';
import { notificationRoutes } from '@app/notifications/notification.routes';
import { profileRoutes } from '@app/profiles/profiles.routes';
import { subscriptionRoutes } from '@app/subscriptions/subscription.routes';

export const routes: Routes = [
  ...marketingRoutes,
  ...authRoutes,
  ...dashboardRoutes,
  ...matchRoutes,
  ...courtRoutes,
  ...profileRoutes,
  ...subscriptionRoutes,
  ...notificationRoutes,
  ...administrationRoutes,
  {
    path: '**',
    redirectTo: ''
  }
];
