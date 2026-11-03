import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SubscriptionPlan } from '@app/subscriptions/subscription.models';
import { SubscriptionService } from '@app/subscriptions/subscription.service';

@Component({
  selector: 'app-player-plans',
  imports: [RouterLink],
  templateUrl: './player-plans.component.html',
  styleUrl: './player-plans.component.scss'
})
export class PlayerPlansComponent implements OnInit {
  plans: SubscriptionPlan[] = [];

  constructor(private subscriptionService: SubscriptionService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.subscriptionService.getPlans().subscribe(plans => {
      this.plans = plans.filter(plan => plan.role === 'JUGADOR');
      this.cdr.detectChanges();
    });
  }

  benefits(code: string): string[] {
    return code === 'JUGADOR_PLUS'
      ? ['Todo lo del plan Básico', 'Insignia Plus en tu cuenta', 'Gestión de renovación', 'Historial de pagos verificados']
      : ['Perfil deportivo', 'Búsqueda y creación de partidos', 'Unión a partidos disponibles', 'Notificaciones de actividad'];
  }
}
