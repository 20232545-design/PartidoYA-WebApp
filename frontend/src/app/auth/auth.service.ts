import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { API_BASE_URL } from '@shared/services/api-base-url.token';
import { AuthResponse, UserResponse } from './auth.models';
import { BrowserSessionStore } from './browser-session.store';

export type { AuthResponse, UserResponse } from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = inject(API_BASE_URL);

  constructor(
    private http: HttpClient,
    private sessionStore: BrowserSessionStore
  ) {}

  register(fullName: string, email: string, password: string, role: string): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.apiUrl}/auth/register`, {
      fullName,
      email,
      password,
      role
    });
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/login`, {
      email,
      password
    }).pipe(
      tap(response => this.sessionStore.saveAuthenticatedUser(response))
    );
  }

  logout(): void {
    this.sessionStore.clear();
  }

  isLoggedIn(): boolean {
    return this.sessionStore.hasToken();
  }

  getUserId(): number | null {
    return this.sessionStore.userId();
  }

  getUserName(): string | null {
    return this.sessionStore.userName();
  }

  getUserEmail(): string | null {
    return this.sessionStore.email();
  }

  getRole(): string | null {
    return this.sessionStore.role();
  }

  getPlan(): string | null {
    return this.sessionStore.plan();
  }

  isPlayerPlan(): boolean {
    const plan = this.getPlan();
    return this.getRole() === 'JUGADOR' && (plan === 'JUGADOR_BASICO' || plan === 'JUGADOR_PLUS');
  }

  isCourtPlan(): boolean {
    const plan = this.getPlan();
    return this.getRole() === 'ADMIN_CANCHA' && (plan === 'CANCHA_EMPRENDEDOR' || plan === 'CANCHA_BUSINESS');
  }

  isPlatformAdmin(): boolean {
    return this.getRole() === 'ADMIN_GENERAL' && this.getPlan() === 'ADMIN_GENERAL';
  }

  dashboardRoute(): string {
    return '/home';
  }

  setPlan(plan: string): void {
    this.sessionStore.updatePlan(plan);
  }

  isAdminCancha(): boolean {
    return this.getRole() === 'ADMIN_CANCHA';
  }
}
