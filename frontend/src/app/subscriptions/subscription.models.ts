export interface SubscriptionPlan {
  code: string;
  displayName: string;
  description: string;
  role: 'JUGADOR' | 'ADMIN_CANCHA';
  amountCents: number;
  currency: string;
  intervalUnit: string;
  paid: boolean;
  subscriptionAvailable: boolean;
}

export interface Subscription {
  id: number;
  planCode: string;
  status: 'PENDING' | 'ACTIVE' | 'PAST_DUE' | 'CANCELED' | 'EXPIRED' | 'FAILED';
  paymentMode: 'ONE_TIME' | 'RECURRING_LEGACY';
  currentPeriodStart: string | null;
  currentPeriodEnd: string | null;
  nextRenewalAt: string | null;
  canceledAt: string | null;
}

export interface SubscriptionPayment {
  id: number;
  planCode: string;
  amountCents: number;
  currency: string;
  status: 'SUCCEEDED' | 'FAILED';
  paidAt: string | null;
  failureReason: string | null;
}

export interface MySubscription {
  currentPlanCode: string;
  legacyAccess: boolean;
  subscription: Subscription | null;
  payments: SubscriptionPayment[];
}
