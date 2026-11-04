import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { API_BASE_URL } from '@shared/services/api-base-url.token';
import { BrowserSessionStore } from '@app/auth/browser-session.store';
import { MySubscription, Subscription, SubscriptionPlan } from './subscription.models';

@Injectable({ providedIn: 'root' })
export class SubscriptionService {
  private readonly apiUrl = inject(API_BASE_URL);
  private readonly http = inject(HttpClient);
  private readonly sessionStore = inject(BrowserSessionStore);

  getPlans(): Observable<SubscriptionPlan[]> {
    return this.http.get<SubscriptionPlan[]>(`${this.apiUrl}/subscription-plans`);
  }

  create(planCode: string, tokenId: string, idempotencyKey: string): Observable<Subscription> {
    return this.http.post<Subscription>(`${this.apiUrl}/subscriptions`, {
      planCode,
      tokenId,
      termsAccepted: true,
      idempotencyKey
    }).pipe(
      tap(result => {
        if (result.status === 'ACTIVE') this.sessionStore.updatePlan(result.planCode);
      })
    );
  }

  getMine(): Observable<MySubscription> {
    return this.http.get<MySubscription>(`${this.apiUrl}/subscriptions/me`).pipe(
      tap(result => this.sessionStore.updatePlan(result.currentPlanCode))
    );
  }

  cancelMine(): Observable<Subscription> {
    return this.http.delete<Subscription>(`${this.apiUrl}/subscriptions/me`).pipe(
      tap(() => this.getMine().subscribe())
    );
  }
}
