import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth.guard';

// loadComponent splits each page into its own bundle, downloaded on first visit (lazy loading).
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'teams' },
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/login').then((m) => m.LoginPage),
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/register').then((m) => m.RegisterPage),
  },
  {
    path: 'teams',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/teams').then((m) => m.TeamsPage),
  },
  {
    path: 'teams/:teamId',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/team-detail').then((m) => m.TeamDetailPage),
  },
  {
    path: 'projects/:projectId',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/board').then((m) => m.BoardPage),
  },
  { path: '**', redirectTo: 'teams' },
];
