import { describe, expect, it } from 'vitest';
import { ReconnectBudget, websocketUrlFrom } from './realtime.service';

describe('websocketUrlFrom', () => {
  it('uses ws for plain http APIs and drops the REST prefix', () => {
    expect(websocketUrlFrom('http://localhost:8080/api/v1')).toBe('ws://localhost:8080/ws');
  });

  it('uses wss for https APIs so production traffic stays encrypted', () => {
    expect(websocketUrlFrom('https://partidoya-api.onrender.com/api/v1/')).toBe('wss://partidoya-api.onrender.com/ws');
  });

  it('resolves relative API URLs against the current origin', () => {
    expect(websocketUrlFrom('/api/v1', 'https://partidoya.pro')).toBe('wss://partidoya.pro/ws');
  });
});

describe('ReconnectBudget', () => {
  it('stops after the configured number of consecutive failures', () => {
    const budget = new ReconnectBudget(3);

    expect(budget.shouldRetryAfterFailure()).toBe(true);
    expect(budget.shouldRetryAfterFailure()).toBe(true);
    expect(budget.shouldRetryAfterFailure()).toBe(false);
    expect(budget.shouldRetryAfterFailure()).toBe(false);
  });

  it('starts a fresh retry sequence after a successful connection', () => {
    const budget = new ReconnectBudget(2);
    expect(budget.shouldRetryAfterFailure()).toBe(true);
    expect(budget.shouldRetryAfterFailure()).toBe(false);

    budget.reset();
    expect(budget.shouldRetryAfterFailure()).toBe(true);
  });
});
