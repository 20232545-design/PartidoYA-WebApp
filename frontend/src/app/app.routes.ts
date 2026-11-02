import { Routes } from '@angular/router';
import { accessRoutes } from '@features/access/access.routes';
import { administrationRoutes } from '@features/administration/administration.routes';
import { courtRoutes } from '@features/courts/court.routes';
import { dashboardRoutes } from '@features/dashboard/dashboard.routes';
import { marketingRoutes } from '@features/marketing/marketing.routes';
import { matchRoutes } from '@features/matches/match.routes';
import { notificationRoutes } from '@features/notifications/notification.routes';
import { profileRoutes } from '@features/profile/profile.routes';

export const routes: Routes = [
  ...marketingRoutes,
  ...accessRoutes,
  ...dashboardRoutes,
  ...matchRoutes,
  ...courtRoutes,
  ...profileRoutes,
  ...notificationRoutes,
  ...administrationRoutes,
  {
    path: '**',
    redirectTo: ''
  }
];
