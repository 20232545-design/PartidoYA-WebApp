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
  selector: 'app-planes-jugador',
  imports: [RouterLink],
  templateUrl: './planes-jugador.component.html',
  styleUrl: './planes-jugador.component.scss'
})
export class PlanesJugadorComponent {
  readonly plans: PlanCard[] = [
    {
      name: 'Jugador Básico',
      price: 'S/ 7.90',
      description: 'Para deportistas casuales que buscan partidos ocasionales.',
      cta: 'Comenzar',
      benefits: [
        'Creación de perfil deportivo',
        'Selección de deportes favoritos',
        'Búsqueda y creación de partidos',
        'Filtros por deporte, fecha y ubicación',
        'Unión a partidos disponibles',
        'Notificaciones básicas'
      ]
    },
    {
      name: 'Jugador Plus',
      price: 'S/ 14.90',
      description: 'Para deportistas frecuentes que juegan varias veces al mes.',
      cta: 'Elegir Plus',
      featured: true,
      benefits: [
        'Todo lo del Básico',
        'Filtros avanzados por nivel y distancia',
        'Prioridad en cupos',
        'Historial de partidos',
        'Reputación de jugadores',
        'Notificaciones personalizadas'
      ]
    }
  ];
}
