import { Routes } from '@angular/router';
import { playerGuard } from '@app/auth/plan.guard';
export const teamRoutes:Routes=[
  {path:'equipos',loadComponent:()=>import('./team-list/team-list.component').then(m=>m.TeamListComponent)},
  {path:'equipos/crear',canActivate:[playerGuard],loadComponent:()=>import('./team-form/team-form.component').then(m=>m.TeamFormComponent)},
  {path:'equipos/:id/editar',canActivate:[playerGuard],loadComponent:()=>import('./team-form/team-form.component').then(m=>m.TeamFormComponent)},
  {path:'equipos/:id/administrar',canActivate:[playerGuard],loadComponent:()=>import('./team-management/team-management.component').then(m=>m.TeamManagementComponent)},
  {path:'equipos/:id',loadComponent:()=>import('./team-detail/team-detail.component').then(m=>m.TeamDetailComponent)},
  {path:'mis-equipos',canActivate:[playerGuard],loadComponent:()=>import('./my-teams/my-teams.component').then(m=>m.MyTeamsComponent)},
  {path:'jugadores/:userId',loadComponent:()=>import('./player-public/player-public.component').then(m=>m.PlayerPublicComponent)}
];
