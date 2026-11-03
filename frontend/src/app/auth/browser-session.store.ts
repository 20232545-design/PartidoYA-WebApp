import { Injectable } from '@angular/core';
import { AuthResponse } from './auth.models';

const SESSION_KEYS = {
  token: 'token',
  userId: 'userId',
  userName: 'userName',
  email: 'email',
  role: 'role',
  plan: 'plan'
} as const;

@Injectable({ providedIn: 'root' })
export class BrowserSessionStore {
  saveAuthenticatedUser(response: AuthResponse): void {
    localStorage.setItem(SESSION_KEYS.token, response.token);
    localStorage.setItem(SESSION_KEYS.userId, response.userId.toString());
    localStorage.setItem(SESSION_KEYS.userName, response.fullName);
    localStorage.setItem(SESSION_KEYS.email, response.email);
    localStorage.setItem(SESSION_KEYS.role, response.role);
    localStorage.setItem(SESSION_KEYS.plan, response.plan);
  }

  clear(): void {
    Object.values(SESSION_KEYS).forEach(key => localStorage.removeItem(key));
  }

  hasToken(): boolean {
    return this.read(SESSION_KEYS.token) !== null;
  }

  token(): string | null {
    return this.read(SESSION_KEYS.token);
  }

  userId(): number | null {
    const storedUserId = this.read(SESSION_KEYS.userId);
    if (storedUserId === null) return null;
    const userId = Number(storedUserId);
    return Number.isInteger(userId) && userId > 0 ? userId : null;
  }

  userName(): string | null {
    return this.read(SESSION_KEYS.userName);
  }

  email(): string | null {
    return this.read(SESSION_KEYS.email);
  }

  role(): string | null {
    return this.read(SESSION_KEYS.role);
  }

  plan(): string | null {
    return this.read(SESSION_KEYS.plan);
  }

  updatePlan(plan: string): void {
    localStorage.setItem(SESSION_KEYS.plan, plan);
  }

  private read(key: string): string | null {
    return localStorage.getItem(key);
  }
}
