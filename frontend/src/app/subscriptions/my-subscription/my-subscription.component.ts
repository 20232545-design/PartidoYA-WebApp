import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom, forkJoin } from 'rxjs';
import { AuthService } from '@app/auth/auth.service';
import { SubscriptionPaymentService } from '../subscription-payment.service';
import { MySubscription, SubscriptionPlan } from '../subscription.models';
import { SubscriptionService } from '../subscription.service';

@Component({
  selector: 'app-my-subscription',
  imports: [CommonModule, RouterLink],
  templateUrl: './my-subscription.component.html',
  styleUrl: './my-subscription.component.scss'
})
export class MySubscriptionComponent implements OnInit {
  private readonly subscriptionService = inject(SubscriptionService);
  private readonly paymentService = inject(SubscriptionPaymentService);
  private readonly authService = inject(AuthService);
  private readonly cdr = inject(ChangeDetectorRef);

  data: MySubscription | null = null;
  plans: SubscriptionPlan[] = [];
  loading = true;
  processing = '';
  message = '';
  errorMessage = '';

  ngOnInit(): void {
    this.load();
  }

  get currentPlan(): SubscriptionPlan | undefined {
    return this.plans.find(plan => plan.code === this.data?.currentPlanCode);
  }

  get availablePlans(): SubscriptionPlan[] {
    const role = this.authService.getRole();
    return this.plans.filter(plan => plan.role === role && plan.paid && plan.code !== this.data?.currentPlanCode);
  }

  get statusLabel(): string {
    const labels: Record<string, string> = {
      PENDING: 'Pendiente de confirmación',
      ACTIVE: 'Activa',
      PAST_DUE: 'Pago pendiente',
      CANCELED: 'Cancelada',
      EXPIRED: 'Vencida',
      FAILED: 'No iniciada'
    };
    return labels[this.data?.subscription?.status || ''] || (this.data?.legacyAccess ? 'Acceso histórico' : 'Sin suscripción');
  }

  async subscribe(plan: SubscriptionPlan): Promise<void> {
    this.processing = plan.code;
    this.message = '';
    this.errorMessage = '';
    try {
      if (!plan.subscriptionAvailable) throw new Error('Culqi aún no está habilitado para este plan. No se realizó ningún cobro.');
      const token = await this.paymentService.captureCardToken(`Suscripción ${plan.displayName}`);
      const result = await firstValueFrom(this.subscriptionService.create(plan.code, token));
      this.message = result.status === 'FAILED'
        ? 'No se pudo iniciar la suscripción. Tu plan anterior no cambió.'
        : 'La suscripción quedó pendiente. El plan se activará cuando Culqi confirme el cobro.';
      this.load();
    } catch (error: any) {
      this.errorMessage = error?.error?.message || error?.message || 'No se pudo iniciar la suscripción.';
    } finally {
      this.processing = '';
      this.cdr.detectChanges();
    }
  }

  cancel(): void {
    if (!window.confirm('¿Confirmas que deseas cancelar tu suscripción?')) return;
    this.processing = 'cancel';
    this.message = '';
    this.errorMessage = '';
    this.subscriptionService.cancelMine().subscribe({
      next: () => {
        this.processing = '';
        this.message = 'La suscripción fue cancelada.';
        this.load();
      },
      error: error => {
        this.processing = '';
        this.errorMessage = error?.error?.message || 'No se pudo cancelar la suscripción.';
        this.cdr.detectChanges();
      }
    });
  }

  formatAmount(amountCents: number): string {
    return `S/ ${(amountCents / 100).toFixed(2)}`;
  }

  private load(): void {
    forkJoin({ data: this.subscriptionService.getMine(), plans: this.subscriptionService.getPlans() }).subscribe({
      next: result => {
        this.data = result.data;
        this.plans = result.plans;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'No pudimos cargar tu suscripción.';
        this.cdr.detectChanges();
      }
    });
  }
}
