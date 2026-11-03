import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SubscriptionPlan } from '@app/subscriptions/subscription.models';
import { SubscriptionService } from '@app/subscriptions/subscription.service';

@Component({
  selector: 'app-court-plans',
  imports: [RouterLink],
  templateUrl: './court-plans.component.html',
  styleUrl: './court-plans.component.scss'
})
export class CourtPlansComponent implements OnInit {
  plans: SubscriptionPlan[] = [];

  constructor(private subscriptionService: SubscriptionService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.subscriptionService.getPlans().subscribe(plans => {
      this.plans = plans.filter(plan => plan.role === 'ADMIN_CANCHA' && plan.paid);
      this.cdr.detectChanges();
    });
  }

  benefits(code: string): string[] {
    return code === 'CANCHA_BUSINESS'
      ? ['Todo lo del plan Emprendedor', 'Gestión de múltiples canchas', 'Reportes de reservas e ingresos', 'Estadísticas de ocupación']
      : ['Gestión de una cancha', 'Administración de horarios', 'Disponibilidad y bloqueos', 'Gestión de reservas'];
  }
}
