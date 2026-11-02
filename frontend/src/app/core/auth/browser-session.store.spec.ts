import { beforeEach, describe, expect, it } from 'vitest';
import { AuthResponse } from './auth.models';
import { BrowserSessionStore } from './browser-session.store';

describe('BrowserSessionStore', () => {
  const authenticatedUser: AuthResponse = {
    token: 'signed-token',
    userId: 42,
    email: 'jugador@example.com',
    fullName: 'Jugador PartidoYA',
    role: 'JUGADOR',
    plan: 'JUGADOR_PLUS'
  };
  let store: BrowserSessionStore;

  beforeEach(() => {
    localStorage.clear();
    store = new BrowserSessionStore();
  });

  it('stores and reads the authenticated user session', () => {
    store.saveAuthenticatedUser(authenticatedUser);

    expect(store.hasToken()).toBe(true);
    expect(store.userId()).toBe(42);
    expect(store.userName()).toBe('Jugador PartidoYA');
    expect(store.email()).toBe('jugador@example.com');
    expect(store.role()).toBe('JUGADOR');
    expect(store.plan()).toBe('JUGADOR_PLUS');
  });

  it('rejects invalid persisted user identifiers', () => {
    localStorage.setItem('userId', 'invalid');

    expect(store.userId()).toBeNull();
  });

  it('clears every session field', () => {
    store.saveAuthenticatedUser(authenticatedUser);

    store.clear();

    expect(store.hasToken()).toBe(false);
    expect(store.userId()).toBeNull();
    expect(store.userName()).toBeNull();
    expect(store.email()).toBeNull();
    expect(store.role()).toBeNull();
    expect(store.plan()).toBeNull();
  });
});
