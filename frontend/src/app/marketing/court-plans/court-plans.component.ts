import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

interface PlanCard {
  name: string;
  price: string;
  description: string;
  cta: string;
  featured?: boolean;
  benefits: string[];
}

@Component({
  selector: 'app-court-plans',
  imports: [RouterLink],
  templateUrl: './court-plans.component.html',
  styleUrl: './court-plans.component.scss'
})
export class CourtPlansComponent {
  readonly plans: PlanCard[] = [
    {
      name: 'Cancha Emprendedor',
      price: 'S/ 29.90',
      description: 'Para canchas pequeñas o independientes.',
      cta: 'Comenzar',
      benefits: [
        'Registro y publicación de cancha',
        'Administración de horarios',
        'Gestión básica de reservas',
        'Visibilidad en búsquedas',
        'Recepción de solicitudes'
      ]
    },
    {
      name: 'Cancha Business',
      price: 'S/ 59.90',
      description: 'Para complejos deportivos con mayor flujo de reservas.',
      cta: 'Elegir Business',
      featured: true,
      benefits: [
        'Todo lo del Emprendedor',
        'Estadísticas de ocupación',
        'Promoción destacada en búsquedas',
        'Gestión avanzada de reservas',
        'Reportes de demanda',
        'Soporte preferente'
      ]
    }
  ];
}
